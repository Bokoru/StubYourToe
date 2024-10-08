package art.bokoru.stubyourtoe;

import com.alrex.parcool.api.SoundEvents;
import com.alrex.parcool.common.action.impl.Roll;
import com.alrex.parcool.common.capability.Parkourability;
import com.alrex.parcool.common.network.StartBreakfallMessage;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
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

    static boolean hasParcool;

    public StubYourToe()
    {
        ModLoadingContext.get().registerConfig(Type.COMMON, Config.SPEC, "stubyourtoe.toml");
        
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        EFFECTS.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Check if the parcool mod is loaded
        hasParcool = false;
        if (ModList.get().isLoaded("parcool"))
        {
            hasParcool = true;
        }
    }

    @SubscribeEvent
    public void onCommandExecuted(CommandEvent event) {
        CommandSourceStack source = event.getParseResults().getContext().getSource();
        if (source.getEntity() instanceof LivingEntity) {
            LivingEntity entity = (LivingEntity) source.getEntity();
            if (entity == null)
                return;

            // Check if the command is the clear command
            String command = event.getParseResults().getReader().getString();
            if (command.equalsIgnoreCase("/effect clear") || command.startsWith("/effect clear ")) {
                entity.getPersistentData().putBoolean("stubbed_toe_cleared", true);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerMovement(TickEvent.PlayerTickEvent playerTickEvent)
    {
        if (playerTickEvent.phase != TickEvent.Phase.END)
            return;

        // Get the player to test for the movement of.
        Player player = playerTickEvent.player;

        // Only execute on server.
        if (player.level().isClientSide())
            return;

        // Players in creative mode should not stub their toe.
        if (player.isCreative())
            return;

        // Don't stub the player's toe if their toe is already stubbed.
        if (player.hasEffect(STUBBED_TOE_TIER1.get()) ||
            player.hasEffect(STUBBED_TOE_TIER2.get()) ||
            player.hasEffect(STUBBED_TOE_TIER3.get()))
            return;

        // Don't stub the player's toe if they are not on the ground.
        if(!player.onGround()) 
            return;

        // Don't stub the player's toe if they are sneaking and stubbing while sneaking is disable.
        if (!Config.enableSneakStub && player.isCrouching())
            return;

        // Don't stub the player's toe if they are in the middle of a roll. 
        if (hasParcool)
        {
            Parkourability parkourability = Parkourability.get(player);
            if (parkourability != null)
            {
                if(parkourability.get(Roll.class).isDoing())
                    return;
            }
        }

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
        boolean hasSteppedUp = deltaX + deltaZ != 0 && deltaY > 0; // Only stub on vertical movement if also moving horizontally.

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

                // Check if the parcool mod is available.
                if (Config.enableRollOnStub && hasParcool)
                {
                    // If so, trigger the roll action.
                    Parkourability parkourability = Parkourability.get(player);
                    if (parkourability != null)
                    {
                        // Play the sound.
                        player.playSound(SoundEvents.ROLL.get(), 1.0f, 1.0f);
                        
                        // Start a roll.
                        StartBreakfallMessage.send((ServerPlayer)player, false);
                    }
                }

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
