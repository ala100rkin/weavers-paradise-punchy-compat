package xox.labvorty.weaversparadise.compat.punchy;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import punchy.config.PunchyConfig;
import xox.labvorty.weaversparadise.items.armor.ModelReplacer;

@Mixin(targets = "punchy.client.render.PunchyArmRenderer")
public abstract class PunchyArmRendererMixin {
    @Shadow
    private static void applyArmMeshOffsets(PoseStack poseStack, boolean leftArm) {
    }

    @Shadow
    private static void applyFreezeShake(PoseStack poseStack, AbstractClientPlayer player, float partialTick) {
    }

    @Shadow
    private static void copyMatrixToSleeve(ModelPart sourceArm, ModelPart targetArm) {
    }

    @Inject(method = "renderArmArmor", at = @At("HEAD"), cancellable = true)
    private static void weaversparadise$renderCosplayArm(
            PlayerModel<?> playerModel,
            AbstractClientPlayer player,
            HumanoidArm arm,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            boolean mirrored,
            float partialTick,
            CallbackInfo ci
    ) {
        if (!PunchyConfig.renderArmorArmsFP()) {
            return;
        }

        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chestStack.getItem() instanceof ArmorItem armorItem)
                || armorItem.getEquipmentSlot() != EquipmentSlot.CHEST) {
            return;
        }

        CosplayCurio.Match match = CosplayCurio.find(player);
        if (match == null) {
            return;
        }

        ModelReplacer replacer = match.replacer();
        ModelPart sourceArm = arm == HumanoidArm.LEFT ? playerModel.leftArm : playerModel.rightArm;
        var cosplayModel = replacer.getModelForChestplate(player, playerModel);
        ModelPart cosplayArm = arm == HumanoidArm.LEFT ? cosplayModel.leftArm : cosplayModel.rightArm;
        if (cosplayArm == null) {
            return;
        }

        cosplayArm.copyFrom(sourceArm);
        copyMatrixToSleeve(sourceArm, cosplayArm);
        cosplayArm.visible = true;

        ResourceLocation texture = replacer.getTextureForSlot(EquipmentSlot.CHEST);
        poseStack.pushPose();
        if (mirrored) {
            poseStack.translate((arm == HumanoidArm.LEFT ? 1.0F : -1.0F) * 0.5F / 16.0F, 0.0F, 0.0F);
        }
        applyArmMeshOffsets(poseStack, arm == HumanoidArm.LEFT);
        applyFreezeShake(poseStack, player, partialTick);
        cosplayArm.render(
                poseStack,
                buffer.getBuffer(RenderType.armorCutoutNoCull(texture)),
                packedLight,
                OverlayTexture.NO_OVERLAY
        );
        poseStack.popPose();

        ci.cancel();
    }
}
