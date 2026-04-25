package dev.astro.command;

import dev.astro.AstroClient;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.Collections;
import java.util.List;

/**
 * /astrokey <key> — Sets your Hypixel API key for LevelHead.
 * Key is saved to astroclient/apikey.txt (never in source code).
 */
public class ApiKeyCommand extends CommandBase {

    @Override
    public String getCommandName() { return "astrokey"; }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/astrokey <your-hypixel-api-key>";
    }

    @Override
    public int getRequiredPermissionLevel() { return 0; }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) { return true; }

    @Override
    public List<String> getCommandAliases() {
        return Collections.singletonList("setapikey");
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) {
            if (AstroClient.INSTANCE.getHypixelAPI().hasKey()) {
                String key = AstroClient.INSTANCE.getHypixelAPI().getApiKey();
                sender.addChatMessage(new ChatComponentText(
                        EnumChatFormatting.AQUA + "[AstroClient] " + EnumChatFormatting.WHITE +
                        "API key is set (" + key.substring(0, 8) + "...). Use " +
                        EnumChatFormatting.YELLOW + "/astrokey <key>" + EnumChatFormatting.WHITE +
                        " to change it."));
            } else {
                sender.addChatMessage(new ChatComponentText(
                        EnumChatFormatting.AQUA + "[AstroClient] " + EnumChatFormatting.RED +
                        "No API key set. Use " + EnumChatFormatting.YELLOW + "/astrokey <key>" +
                        EnumChatFormatting.RED + " — get one with " + EnumChatFormatting.GREEN +
                        "/api new" + EnumChatFormatting.RED + " on Hypixel."));
            }
            return;
        }

        String key = args[0].trim();
        // Basic UUID format validation
        if (key.length() != 36 || key.split("-").length != 5) {
            sender.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.AQUA + "[AstroClient] " + EnumChatFormatting.RED +
                    "Invalid key format. Run " + EnumChatFormatting.GREEN + "/api new" +
                    EnumChatFormatting.RED + " on Hypixel to get a valid key."));
            return;
        }

        AstroClient.INSTANCE.getHypixelAPI().setApiKey(key);
        sender.addChatMessage(new ChatComponentText(
                EnumChatFormatting.AQUA + "[AstroClient] " + EnumChatFormatting.GREEN +
                "API key saved! LevelHead will now use the Hypixel API."));
    }
}
