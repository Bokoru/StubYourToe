package art.bokoru.stubyourtoe;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = StubYourToe.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config 
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.DoubleValue BASE_STUB_CHANCE = BUILDER
        .comment("The percent chance that a player will stub their toe when crossing the edge of a block.")
        .defineInRange("baseStubChance", 0.0001d, Double.MIN_VALUE, 100.0d);

    private static final ForgeConfigSpec.DoubleValue STEP_UP_STUB_MODIFIER = BUILDER
        .comment("The modifier for the chance that a player will stub their toe when \"stepping up\" from a lower y-level to a higher y-level. (Ex: stairs, slabs, etc.)")
        .defineInRange("stepUpStubModifier", 2.0, 0.01, 100.0d);

    private static final ForgeConfigSpec.BooleanValue ENABLE_SNEAK_STUB = BUILDER
        .comment("If enabled, the player can stub their toe even while sneaking.")
        .define("enableSneakStub", false);

    private static final ForgeConfigSpec.BooleanValue ENABLE_ROLL_ON_STUB = BUILDER
        .comment("If enabled, the player will enter a roll when they stub their toe. This setting only applies if the \"ParCool! ~ Minecraft Parkour ~\" mod is installed.")
        .define("enableRollOnStub", true);

    private static final ForgeConfigSpec.DoubleValue STUBBED_TOE_SPEED_MODIFIER = BUILDER
        .comment("The speed modifier applied to a player with the Stubbed Toe status effect.")
        .defineInRange("stubbedToeSpeedModifier", 0.25d, Double.MIN_VALUE, 1.0d);

    private static final ForgeConfigSpec.IntValue FIRST_TIER_DURATION_TICKS = BUILDER
        .comment("The duration (in ticks) that the first tier of the status effect will last.")
        .defineInRange("tier1DurationTicks", 1300, 1, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue SECOND_TIER_DURATION_TICKS = BUILDER
        .comment("The duration (in ticks) that the second tier of the status effect will last.")
        .defineInRange("tier2DurationTicks", 400, 1, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue THIRD_TIER_DURATION_TICKS = BUILDER
        .comment("The duration (in ticks) that the third tier of the status effect will last.")
        .defineInRange("tier3DurationTicks", 100, 1, Integer.MAX_VALUE);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static double baseStubChance;
    public static double stepUpStubModifier;
    public static boolean enableSneakStub;
    public static boolean enableRollOnStub;
    public static double stubbedToeSpeedModifier;
    public static int tier1DurationTicks;
    public static int tier2DurationTicks;
    public static int tier3DurationTicks;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        baseStubChance = BASE_STUB_CHANCE.get();
        stepUpStubModifier = STEP_UP_STUB_MODIFIER.get();

        enableSneakStub = ENABLE_SNEAK_STUB.get();
        enableRollOnStub = ENABLE_ROLL_ON_STUB.get();

        stubbedToeSpeedModifier = STUBBED_TOE_SPEED_MODIFIER.get();

        tier1DurationTicks = FIRST_TIER_DURATION_TICKS.get();
        tier2DurationTicks = SECOND_TIER_DURATION_TICKS.get();
        tier3DurationTicks = THIRD_TIER_DURATION_TICKS.get();
    }
}
