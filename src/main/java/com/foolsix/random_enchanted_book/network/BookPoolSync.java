package com.foolsix.random_enchanted_book.network;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.foolsix.random_enchanted_book.RandomEnchantedBook;
import com.foolsix.random_enchanted_book.item.ModItemReg;
import io.netty.util.AttributeKey;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public final class BookPoolSync {
    private static final AttributeKey<Snapshot> CLIENT_POOLS = AttributeKey.valueOf(Snapshot.TYPE.id().toString());

    private BookPoolSync() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(Snapshot.TYPE, Snapshot.STREAM_CODEC, BookPoolSync::receive);
    }

    public static void onDatapackSync(OnDatapackSyncEvent event) {
        sendPools(event.getPlayerList().getServer(), event.getRelevantPlayers());
    }

    public static void onConfigReload() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.execute(() -> sendPools(server, server.getPlayerList().getPlayers().stream()));
        }
    }

    private static void sendPools(MinecraftServer server, Stream<ServerPlayer> players) {
        List<ServerPlayer> recipients = players.filter(player -> player.connection.hasChannel(Snapshot.TYPE)).toList();
        if (recipients.isEmpty()) {
            return;
        }

        RegistryAccess registries = server.registryAccess();
        Map<ResourceLocation, List<ResourceLocation>> pools = new LinkedHashMap<>();
        for (var book : ModItemReg.BOOKS) {
            List<ResourceLocation> enchantments = book.get().getEnchantmentPool(registries).stream()
                    .map(enchantment -> enchantment.key().location())
                    .sorted()
                    .toList();
            pools.put(book.getId(), enchantments);
        }
        Snapshot snapshot = new Snapshot(pools);
        recipients.forEach(player -> PacketDistributor.sendToPlayer(player, snapshot));
    }

    private static void receive(Snapshot snapshot, IPayloadContext context) {
        context.connection().channel().attr(CLIENT_POOLS).set(snapshot);
    }

    public static Optional<List<ResourceLocation>> getClientPool(Connection connection, ResourceLocation book) {
        Snapshot snapshot = connection.channel().attr(CLIENT_POOLS).get();
        return snapshot == null ? Optional.empty() : Optional.ofNullable(snapshot.pools().get(book));
    }

    private record Snapshot(Map<ResourceLocation, List<ResourceLocation>> pools) implements CustomPacketPayload {
        private static final Type<Snapshot> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(RandomEnchantedBook.MODID, "book_pools"));
        private static final StreamCodec<FriendlyByteBuf, Snapshot> STREAM_CODEC =
                StreamCodec.ofMember(Snapshot::write, Snapshot::read);

        @Override
        public Type<Snapshot> type() {
            return TYPE;
        }

        private void write(FriendlyByteBuf buffer) {
            buffer.writeMap(pools, FriendlyByteBuf::writeResourceLocation,
                    (buf, enchantments) -> buf.writeCollection(enchantments, FriendlyByteBuf::writeResourceLocation));
        }

        private static Snapshot read(FriendlyByteBuf buffer) {
            return new Snapshot(buffer.readMap(FriendlyByteBuf::readResourceLocation,
                    buf -> buf.readList(FriendlyByteBuf::readResourceLocation)));
        }
    }
}
