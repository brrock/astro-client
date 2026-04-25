package dev.astro.module.render;

import dev.astro.module.Category;
import dev.astro.module.HUDModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.ResourcePackRepository;

import java.util.List;

public final class PackDisplay extends HUDModule {

    public PackDisplay() {
        super("Pack Display", "Shows the active resource pack name.", Category.RENDER, 5, 360);
    }

    @Override
    protected String getText() {
        List<ResourcePackRepository.Entry> entries =
                Minecraft.getMinecraft().getResourcePackRepository().getRepositoryEntries();
        if (entries == null || entries.isEmpty()) return "Default";
        return entries.get(0).getResourcePackName();
    }
}
