package thelm.jaopca.items;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

import com.google.common.collect.TreeBasedTable;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.Rarity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
import thelm.jaopca.JAOPCA;
import thelm.jaopca.api.forms.IForm;
import thelm.jaopca.api.items.IItemFormSettings;
import thelm.jaopca.api.items.IItemFormType;
import thelm.jaopca.api.items.IItemInfo;
import thelm.jaopca.api.items.MaterialFormItem;
import thelm.jaopca.api.materials.IMaterial;
import thelm.jaopca.custom.json.EnumDeserializer;
import thelm.jaopca.custom.json.ItemFormSettingsDeserializer;
import thelm.jaopca.data.DataInjector;
import thelm.jaopca.forms.FormTypeHandler;
import thelm.jaopca.utils.ApiImpl;

public class ItemFormType implements IItemFormType {

	private ItemFormType() {};

	public static final ItemFormType INSTANCE = new ItemFormType();
	private static final TreeSet<IForm> FORMS = new TreeSet<>();
	private static final TreeBasedTable<IForm, IMaterial, MaterialFormItem> ITEMS = TreeBasedTable.create();
	private static final TreeBasedTable<IForm, IMaterial, IItemInfo> ITEM_INFOS = TreeBasedTable.create();
	private static ItemGroup itemGroup;

	public static void init() {
		FormTypeHandler.registerFormType(INSTANCE);
	}

	@Override
	public String getName() {
		return "item";
	}

	@Override
	public String getTranslationKeyFormat() {
		return "item.jaopca.%s";
	}

	@Override
	public void addForm(IForm form) {
		FORMS.add(form);
	}

	@Override
	public Set<IForm> getForms() {
		return Collections.unmodifiableNavigableSet(FORMS);
	}

	@Override
	public boolean shouldRegister(IForm form, IMaterial material) {
		if(material.getType().isDummy()) {
			return true;
		}
		ResourceLocation tagLocation = new ResourceLocation("forge", form.getSecondaryName()+'/'+material.getName());
		return !ApiImpl.INSTANCE.getItemTags().contains(tagLocation);
	}

	@Override
	public IItemInfo getMaterialFormInfo(IForm form, IMaterial material) {
		IItemInfo info = ITEM_INFOS.get(form, material);
		if(info == null && ITEMS.contains(form, material)) {
			info = new ItemInfo(ITEMS.get(form, material));
			ITEM_INFOS.put(form, material, info);
		}
		return info;
	}

	@Override
	public IItemFormSettings getNewSettings() {
		return new ItemFormSettings();
	}

	@Override
	public GsonBuilder configureGsonBuilder(GsonBuilder builder) {
		return builder.registerTypeAdapter(Rarity.class, EnumDeserializer.INSTANCE);
	}

	@Override
	public IItemFormSettings deserializeSettings(JsonElement jsonElement, JsonDeserializationContext context) {
		return ItemFormSettingsDeserializer.INSTANCE.deserialize(jsonElement, context);
	}

	public static void registerItems(IForgeRegistry<Item> registry) {
		for(IForm form : FORMS) {
			IItemFormSettings settings = (IItemFormSettings)form.getSettings();
			for(IMaterial material : form.getMaterials()) {
				boolean isMaterialDummy = material.getType().isDummy();
				MaterialFormItem item = settings.getItemCreator().create(form, material, ()->(IItemFormSettings)form.getSettings());
				String registryKey = isMaterialDummy ? material.getName()+form.getName() : form.getName()+'.'+material.getName();
				item.setRegistryName(new ResourceLocation(JAOPCA.MOD_ID, registryKey));
				registry.register(item);
				ITEMS.put(form, material, item);
				if(!isMaterialDummy) {
					Supplier<Item> supplier = ()->item;
					DataInjector.registerItemTag(new ResourceLocation("forge", form.getSecondaryName()), supplier);
					DataInjector.registerItemTag(new ResourceLocation("forge", form.getSecondaryName()+'/'+material.getName()), supplier);
					for(String alternativeName : material.getAlternativeNames()) {
						DataInjector.registerItemTag(new ResourceLocation("forge", form.getSecondaryName()+'/'+alternativeName), supplier);
					}
				}
			}
		}
	}

	public static ItemGroup getItemGroup() {
		if(itemGroup == null) {
			itemGroup = new ItemGroup("jaopca") {
				@Override
				public ItemStack createIcon() {
					return new ItemStack(Items.GLOWSTONE_DUST);
				}
			};
		}
		return itemGroup;
	}

	public static Collection<MaterialFormItem> getItems() {
		return ITEMS.values();
	}
}
