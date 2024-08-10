package art.bokoru.stubyourtoe;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(StubYourToe.MODID)
public class StubYourToe
{
    public static final String MODID = "stubyourtoe";

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, MODID);

    public static final RegistryObject<MobEffect> STUBBED_TOE_TIER1 = EFFECTS.register("stubbed_toe1", () -> new StubbedToe(1, false, false, null));
    public static final RegistryObject<MobEffect> STUBBED_TOE_TIER2 = EFFECTS.register("stubbed_toe2", () -> new StubbedToe(2, false, true, STUBBED_TOE_TIER1));
    public static final RegistryObject<MobEffect> STUBBED_TOE_TIER3 = EFFECTS.register("stubbed_toe3", () -> new StubbedToe(3, true, true, STUBBED_TOE_TIER2));

    public static final ResourceKey<DamageType> TOE_STUBBED = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.of("stubyourtoe:toe_stubbed", ':'));

    public StubYourToe()
    {
        ModLoadingContext.get().registerConfig(Type.COMMON, Config.SPEC, "stubyourtoe.toml");
        
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        EFFECTS.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onPlayerMovement(TickEvent.PlayerTickEvent playerTickEvent)
    {
        if (playerTickEvent.phase != TickEvent.Phase.END)
            return;

        // Get the player to test for the movement of.
        Player player = playerTickEvent.player;

        // This logic should only be handled on the server.
        if (player.level().isClientSide()) 
            return;

        // Players in creative mode should not stub their toe.
        if (player.isCreative())
            return;

        // Don't stub the player's toe if they are not on the ground.
        if(!player.onGround()) 
            return;

        // Don't stub the player's toe if they are sneaking and stubbing while sneaking is disable.
        if (!Config.enableSneakStub && player.isCrouching())
            return;

        // Track previous position
        double prevX = player.getPersistentData().getDouble("stubToe_prevX");
        double prevY = player.getPersistentData().getDouble("stubToe_prevY");
        double prevZ = player.getPersistentData().getDouble("stubToe_prevZ");

        // Current position
        double currentX = player.getX();
        double currentY = player.getY();
        double currentZ = player.getZ();

        // Calculate movement
        double deltaX = currentX - prevX;
        double deltaY = currentY - prevY;
        double deltaZ = currentZ - prevZ;

        boolean hasMovedHorizontally = prevX != currentX || prevZ != currentZ;
        boolean hasSteppedUp = deltaY > 0;

        // If the player has moved over the edge of a block, or stepped up from a lower Y.
        if (hasMovedHorizontally || hasSteppedUp) {
            // Calculate the speed that the player was moving at.
            double speed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

            // Calculate the chance that the player will stub their toe.
            double chance = Config.baseStubChance;
            if (hasSteppedUp) {
                // Apply step-up modifier if the player has stepped up.
                chance *= Config.stepUpStubModifier;
            }

            // Apply the effect if the player got unlucky (or lucky if they enjoy it.)
            Level level = player.level();
            if (level.random.nextDouble() < chance) {
                StubbedToe effect;

                if (speed > 0.4) { 
                    // Apply tier 3 if faster than sprinting.
                    effect = (StubbedToe)StubYourToe.STUBBED_TOE_TIER3.get();
                } else { 
                    if (speed > 0.25) {
                        // Apply tier 2 if sprinting.
                        effect = (StubbedToe)StubYourToe.STUBBED_TOE_TIER2.get();
                    } else {
                        // Apply tier 1 if not.
                        effect = (StubbedToe)StubYourToe.STUBBED_TOE_TIER1.get();
                    }
                }

                // Locate the damage type for TOE_STUBBED.
                Registry<DamageType> damageTypes = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
                Holder<DamageType> damageType = damageTypes.getHolderOrThrow(TOE_STUBBED);

                // Damage the player.
                player.hurt(new DamageSource(damageType), 1);

                // Create and apply the effect.
                MobEffectInstance effectInstance = new MobEffectInstance(effect, effect.getDuration(), 0, false, true, true);
                player.addEffect(effectInstance);
            }

            // Store current position for the next check.
            player.getPersistentData().putDouble("stubToe_prevX", currentX);
            player.getPersistentData().putDouble("stubToe_prevY", currentY);
            player.getPersistentData().putDouble("stubToe_prevZ", currentZ);
        }
    }
}
