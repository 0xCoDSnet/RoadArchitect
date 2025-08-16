package net.oxcodsnet.roadarchitect.neoforge.datagen;

import net.minecraft.data.DataOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.datagen.RALanguage;

/**
 * Language provider delegating to common translation table.
 */
public class RoadLanguageProvider extends LanguageProvider {
    private final String code;

    public RoadLanguageProvider(DataOutput output, String code) {
        super(output, RoadArchitect.MOD_ID, code);
        this.code = code;
    }

    @Override
    protected void addTranslations() {
        RALanguage.fill(this.code, this::add);
    }
}

