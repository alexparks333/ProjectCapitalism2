package com.alexh.capitalism.gametest;

import com.alexh.capitalism.Capitalism;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

// The mod's game tests, run headless by `gradlew runGameTestServer` (its own world in runs/gameTestServer; runs every
// test and quits). Test classes add their tests here (register) from a static block; init() loads them before the
// registries fill. Every check is logged as "[ModTest] PASS/FAIL <test>: <what>". Tests run in an empty 5x5x5 box
// (data/capitalism/structure/empty.nbt, tools/make_test_structure.js) with real survival players on a mock
// connection that has every mod channel. Test classes: librarian/SkillBookTests, player/FoodDrinkTests, town/TownTests.
@EventBusSubscriber(modid = Capitalism.MODID)
public final class GameTests {

    private static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, Capitalism.MODID);
    private static final Map<String, ResourceKey<Consumer<GameTestHelper>>> TESTS = new LinkedHashMap<>();

    private GameTests() {
    }

    // Loads the test classes (their static blocks register their tests). Called from the mod's constructor.
    public static void init() {
        com.alexh.capitalism.librarian.SkillBookTests.init();
        com.alexh.capitalism.player.FoodDrinkTests.init();
        com.alexh.capitalism.town.TownTests.init();
        com.alexh.capitalism.town.FishermanTests.init();
        com.alexh.capitalism.pets.PetTests.init();
        com.alexh.capitalism.designer.DesignerTests.init();
        com.alexh.capitalism.farmer.FarmerTests.init();
        com.alexh.capitalism.weaponsmith.SocketTests.init();
        com.alexh.capitalism.realestate.RealEstateTests.init();
        com.alexh.capitalism.boats.BoatTests.init();
        com.alexh.capitalism.calendar.CalendarTests.init();
        com.alexh.capitalism.decor.DecorTests.init();
        com.alexh.capitalism.decor.BloodDecorTests.init();
        com.alexh.capitalism.fishing.FishingTests.init();
        com.alexh.capitalism.bounty.BountyTests.init();
        com.alexh.capitalism.craving.CravingTests.init();
        com.alexh.capitalism.town.MayoralChestTests.init();
        com.alexh.capitalism.deathtax.DeathTaxTests.init();
    }

    public static void register(String name, Consumer<Checks> body) {
        TEST_FUNCTIONS.register(name, () -> helper -> {
            // every test starts on an ordinary day (Tuesday, Spring 2, noon: no festival), so festivals don't touch prices
            com.alexh.capitalism.calendar.Calendar.setClock(helper.getLevel(), com.alexh.capitalism.calendar.Calendar.DAY_TICKS + 6000L);
            Checks checks = new Checks(helper, name);
            try {
                body.accept(checks);
            } catch (RuntimeException e) {
                checks.fail("threw " + e);
                LOGGER.error("[ModTest] " + name + " threw", e);
            }
            checks.finish();
        });
        TESTS.put(name, ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(Capitalism.MODID, name)));
    }

    @SubscribeEvent
    public static void onRegisterTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
            Identifier.fromNamespaceAndPath(Capitalism.MODID, "default"), new TestEnvironmentDefinition.AllOf(List.of()));
        for (var entry : TESTS.entrySet()) {
            event.registerTest(Identifier.fromNamespaceAndPath(Capitalism.MODID, entry.getKey()),
                new FunctionGameTestInstance(entry.getValue(),
                    new TestData<>(environment, Identifier.fromNamespaceAndPath(Capitalism.MODID, "empty"), 100, 0, true)));
        }
    }

    // One test's checks: counts passes, logs each check, fails the test with the list of what failed
    public static final class Checks {
        public final GameTestHelper helper;
        private final String name;
        private final List<String> failures = new ArrayList<>();
        private int passes;

        Checks(GameTestHelper helper, String name) {
            this.helper = helper;
            this.name = name;
        }

        public ServerLevel level() {
            return this.helper.getLevel();
        }

        // A point in the test box (relative x, y, z)
        public Vec3 at(double x, double y, double z) {
            return this.helper.absoluteVec(new Vec3(x, y, z));
        }

        public void check(boolean ok, String what) {
            if (ok) {
                this.passes++;
                LOGGER.info("[ModTest] PASS {}: {}", this.name, what);
            } else {
                this.fail(what);
            }
        }

        public void near(double actual, double expected, double tolerance, String what) {
            this.check(Math.abs(actual - expected) <= tolerance, String.format(Locale.ROOT, "%s (expected %.4f, got %.4f)", what, expected, actual));
        }

        public void fail(String what) {
            this.failures.add(what);
            LOGGER.error("[ModTest] FAIL {}: {}", this.name, what);
        }

        void finish() {
            LOGGER.info("[ModTest] {}: {} passed, {} failed", this.name, this.passes, this.failures.size());
            if (this.failures.isEmpty()) this.helper.succeed();
            else this.helper.fail(this.failures.size() + " failed: " + String.join("; ", this.failures));
        }

        // A fresh survival player standing in the middle of the test box
        public ServerPlayer player() {
            return this.player("mod-test", UUID.randomUUID());
        }

        // ...with this name and id (a second one with the same id is the same player logging in again)
        public ServerPlayer player(String name, UUID id) {
            ServerLevel level = this.level();
            CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(id, name), false);
            ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
                @Override
                public GameType gameMode() {
                    return GameType.SURVIVAL;
                }
            };
            Connection connection = new Connection(PacketFlow.SERVERBOUND);
            new EmbeddedChannel(connection);
            // act like a NeoForge client that has every mod channel (so the mod's sync packets can be sent)
            NetworkRegistry.configureMockConnection(connection);
            level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
            // a real survival player: can be hurt and targeted, gets hungry
            player.setGameMode(GameType.SURVIVAL);
            // the server keeps a player invulnerable until their client says it has loaded the world
            player.connection.markClientLoaded();
            // no cravings today (craving/Cravings): otherwise a test's meal or drink could happen to be one and fill
            // every gold bar (CravingTests sets its own)
            com.alexh.capitalism.craving.Cravings.set(player, "none", -1, "none");
            Vec3 at = this.at(2.5, 2, 2.5);
            player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
            return player;
        }

        // Eat / drink this the way a player does (start using it, then finish): fires the same events as in game.
        // Returns what's left in the hand (an empty bottle, a bowl...).
        public ItemStack consume(ServerPlayer player, ItemStack stack) {
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.startUsingItem(InteractionHand.MAIN_HAND);
            if (!player.isUsingItem()) return player.getMainHandItem();
            try {
                Method complete = LivingEntity.class.getDeclaredMethod("completeUsingItem");
                complete.setAccessible(true);
                complete.invoke(player);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("can't finish using an item", e);
            }
            return player.getMainHandItem();
        }
    }
}
