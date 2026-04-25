package dev.astro.util;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects other AstroClient users on the same server.
 *
 * Sends and listens on a lightweight Forge plugin channel. Compatible servers
 * can forward these packets so clients can identify one another. Recognized
 * players get the Astro marker in nametags and the tab list.
 */
public final class AstroUsers {

    private static final String CHANNEL = "AC|Hello";
    private static final byte[] HELLO = {0x41, 0x43}; // "AC"
    private static final String TAB_PREFIX = "\u00a7b\u26A1 \u00a7r";
    private static final long HELLO_INTERVAL_MS = 5000L;
    private static final long TAB_REFRESH_INTERVAL_MS = 1000L;

    /** Players detected as AstroClient users this session. */
    private final Set<UUID> detected = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());

    private FMLEventChannel channel;
    private long lastHelloSent;
    private long lastTabRefresh;

    public void init() {
        channel = NetworkRegistry.INSTANCE.newEventDrivenChannel(CHANNEL);
        channel.register(this);
        FMLCommonHandler.instance().bus().register(this);
        System.out.println("[AstroClient] User detection ready");
    }

    /** Check if a player is a known AstroClient user. */
    public boolean isAstroUser(UUID uuid) {
        return detected.contains(uuid);
    }

    public Set<UUID> getDetected() {
        return Collections.unmodifiableSet(detected);
    }

    public String getMarkerPrefix() {
        return TAB_PREFIX;
    }

    @SubscribeEvent
    public void onConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        detected.clear();
        lastHelloSent = 0L;
        lastTabRefresh = 0L;
        sendHello();
    }

    @SubscribeEvent
    public void onClientPacket(FMLNetworkEvent.ClientCustomPacketEvent event) {
        if (!CHANNEL.equals(event.packet.channel())) return;

        try {
            PacketBuffer buf = new PacketBuffer(event.packet.payload().copy());
            if (buf.readableBytes() < 2) return;

            byte magicA = buf.readByte();
            byte magicC = buf.readByte();
            if (magicA != HELLO[0] || magicC != HELLO[1]) return;
            if (buf.readableBytes() < 16) return;

            UUID uuid = new UUID(buf.readLong(), buf.readLong());
            if (Minecraft.getMinecraft().thePlayer != null
                    && !uuid.equals(Minecraft.getMinecraft().thePlayer.getUniqueID())) {
                detected.add(uuid);
            }
        } catch (Exception ignored) {
            // Ignore malformed packets from other mods or older Astro builds.
        }
    }

    public void onDisconnect() {
        detected.clear();
        refreshTabMarkers();
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        onDisconnect();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null || mc.getNetHandler() == null) return;

        long now = System.currentTimeMillis();
        if (now - lastHelloSent >= HELLO_INTERVAL_MS) {
            sendHello();
        }
        if (now - lastTabRefresh >= TAB_REFRESH_INTERVAL_MS) {
            refreshTabMarkers();
            lastTabRefresh = now;
        }
    }

    private void sendHello() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        try {
            PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
            buf.writeBytes(HELLO);
            buf.writeLong(mc.thePlayer.getUniqueID().getMostSignificantBits());
            buf.writeLong(mc.thePlayer.getUniqueID().getLeastSignificantBits());
            buf.writeString(mc.thePlayer.getName());
            channel.sendToServer(new FMLProxyPacket(buf, CHANNEL));
            lastHelloSent = System.currentTimeMillis();
        } catch (Exception ignored) {
            // Server doesn't support this channel or blocked it.
        }
    }

    private void refreshTabMarkers() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getNetHandler() == null) return;

        for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
            UUID uuid = info.getGameProfile().getId();
            if (uuid == null) continue;

            String current = getCurrentDisplayText(info);
            boolean shouldMark = detected.contains(uuid);
            boolean isMarked = current.startsWith(TAB_PREFIX);
            String base = isMarked ? current.substring(TAB_PREFIX.length()) : current;

            if (shouldMark && !isMarked) {
                setTabDisplayName(info, TAB_PREFIX + base);
            } else if (!shouldMark && isMarked) {
                setTabDisplayName(info, base);
            }
        }
    }

    private static String getCurrentDisplayText(NetworkPlayerInfo info) {
        if (info.getDisplayName() != null) {
            String text = info.getDisplayName().getFormattedText();
            if (text != null && !text.isEmpty()) return text;
        }
        return info.getGameProfile().getName();
    }

    private static void setTabDisplayName(NetworkPlayerInfo info, String text) {
        ChatComponentText component = text == null ? null : new ChatComponentText(text);

        try {
            Method m = NetworkPlayerInfo.class.getDeclaredMethod("setDisplayName", net.minecraft.util.IChatComponent.class);
            m.invoke(info, component);
            return;
        } catch (Exception ignored) {
        }

        try {
            java.lang.reflect.Field f = NetworkPlayerInfo.class.getDeclaredField("displayName");
            f.setAccessible(true);
            f.set(info, component);
        } catch (Exception ignored) {
        }
    }
}
