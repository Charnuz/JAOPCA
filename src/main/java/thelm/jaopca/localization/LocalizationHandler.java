package thelm.jaopca.localization;

import java.util.Objects;
import java.util.TreeMap;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import thelm.jaopca.api.localization.ILocalizer;

public class LocalizationHandler {

	private static final TreeMap<String, ILocalizer> LOCALIZERS = new TreeMap<>();

	public static void registerLocalizer(ILocalizer localizer, String... languages) {
		Objects.requireNonNull(localizer);
		for(String language : Objects.requireNonNull(languages)) {
			LOCALIZERS.put(language, localizer);
		}
	}

	public static ILocalizer getCurrentLocalizer() {
		String language = "en_us";
		language = DistExecutor.callWhenOn(Dist.CLIENT, ()->()->{
			Minecraft mc = Minecraft.getInstance();
			if(mc != null) {
				return mc.getLanguageManager().getCurrentLanguage().getCode();
			}
			return "en_us";
		});
		return LOCALIZERS.computeIfAbsent(language, key->LocalizerDefault.INSTANCE);
	}
}
