package art.bokoru.stubyourtoe;

import java.util.UUID;

import javax.annotation.Nonnull;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.RegistryObject;

public class StubbedToe extends MobEffect {

    private final RegistryObject<MobEffect> previousTier;
    private final boolean disableSprint;
    private final boolean disableMovement;
    private final int tier;

    private final UUID ATTRIBUTE_UUID = UUID.fromString("829E4C6F-E4AA-41BA-B65E-DF90DB7B79FC");

    protected StubbedToe(int tier, boolean disableMovement, boolean disableSprint, RegistryObject<MobEffect> previousTier) {
        super(MobEffectCategory.HARMFUL, 0xE68ECB); // RGB: 230, 142, 203

        this.disableMovement = disableMovement;
        this.disableSprint = disableSprint;
        this.previousTier = previousTier;
        this.tier = tier;
    }

    public int getDuration() {
        switch(tier)
        {
            default:
            case 1:
                return Config.tier1DurationTicks;

            case 2:
                return Config.tier2DurationTicks;

            case 3:
                return Config.tier3DurationTicks;
        }
    }
    
    @Override
    public void applyEffectTick(@Nonnull LivingEntity entity, int amplifier) {

        // Ensure only the highest tier of stubbed toe is active at any moment.
        if (previousTier != null)
        {
            StubbedToe previous = (StubbedToe)previousTier.get();
            if (entity.hasEffect(previous))
            {
                entity.removeEffect(previous);
            }

            if (previous.previousTier != null)
            {
                previous = (StubbedToe)previous.previousTier.get();
                if (entity.hasEffect(previous))
                {
                    entity.removeEffect(previous);
                }
            }
        }

        // Ensure the correct speed modifier is applied.
        AttributeInstance speedAttribute = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute != null)
        {
            double speed = disableMovement ? -1.0d : (-1.0d + Config.stubbedToeSpeedModifier);

            boolean addModifier = false;
            AttributeModifier modifier = speedAttribute.getModifier(ATTRIBUTE_UUID);
            if (modifier == null)
            {
                addModifier = true;
            }
            else
            {
                if (modifier.getAmount() != speed)
                {
                    speedAttribute.removeModifier(modifier);
                    addModifier = true;
                }
            }

            if (addModifier)
            {
                AttributeModifier attribute = new AttributeModifier(ATTRIBUTE_UUID, "STUBBED_TOE", speed, Operation.MULTIPLY_TOTAL);
                speedAttribute.addTransientModifier(attribute);
            }
        }

        // Ensure the player is not sprinting if it is disabled.
        if (disableSprint)
        {
            if (entity instanceof Player player) {
                player.setSprinting(false);
            }
        }
    }

    @Override
    public void removeAttributeModifiers(@Nonnull LivingEntity entity, @Nonnull AttributeMap attributeMap, int amplifier) {
        super.removeAttributeModifiers(entity, attributeMap, amplifier);

        // Remove any previous speed modifications done with this effect.
        AttributeInstance speedAttribute = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute != null)
        {
            speedAttribute.removeModifier(ATTRIBUTE_UUID);
        }

        // Apply previous tier if available.
        if (previousTier != null)
        {
            StubbedToe effect = (StubbedToe)previousTier.get();
            entity.addEffect(new MobEffectInstance(effect, effect.getDuration(), 0, false, true, true));
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // Ensure this runs every tick.
        return duration > 0;
    }
}
