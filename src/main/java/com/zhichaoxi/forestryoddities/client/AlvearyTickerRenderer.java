package com.zhichaoxi.forestryoddities.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.zhichaoxi.forestryoddities.content.alveary.AlvearyTickerBlockEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

/**
 * Draws the running vanilla clock on the four sides of the ticker.
 *
 * <p>The block texture carries the clock item's first frame, which is what makes the item icon and a
 * static screenshot look right. A baked model cannot animate per block - all instances of a model
 * share one texture - so the block entity renderer draws the frame the clock is on at this moment as
 * a quad just in front of each side. The frame comes from Minecraft's own
 * {@code textures/item/clock_00.png} ... {@code clock_63.png}, so the part really shows the vanilla
 * clock; only the rate is the ticker's, and {@link TickerClock} scales it with the speed setting. The
 * block entity's speed rides on its update tag, so the client knows how fast to run the clock without
 * any per-tick packet.</p>
 *
 * <p>A face is only painted when the block's own face would be rendered
 * ({@link Block#shouldRenderFace}); an alveary wall block that touches another block on one side must
 * not paint a clock onto its neighbour's face.</p>
 */
public class AlvearyTickerRenderer implements BlockEntityRenderer<AlvearyTickerBlockEntity> {
    /** How far the clock sits in front of the face, to win against the face's own depth. */
    private static final float FACE_OFFSET = 0.001F;
    /** Half the block's width, i.e. the half-size of the clock quad. */
    private static final float HALF = 0.5F;

    public AlvearyTickerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(AlvearyTickerBlockEntity ticker, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Level level = ticker.getLevel();
        if (level == null) {
            return;
        }

        BlockPos pos = ticker.getBlockPos();
        BlockState state = ticker.getBlockState();
        int frame = TickerClock.frame(level.getGameTime(), partialTick, ticker.speed(), ticker.maxSpeed());
        ResourceLocation texture = TickerClock.frameTexture(frame);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighbour = pos.relative(direction);
            if (!Block.shouldRenderFace(state, level, pos, direction, neighbour)) {
                continue;
            }

            // An alveary part is a full solid block, and a solid block stores no light of its own:
            // asking for the light at the part's position returns 0 and paints the clock black. The
            // quad hangs in the air cell in front of the face, so that cell's light is the correct
            // one - it is also what the block model's own face is lit with.
            int light = LevelRenderer.getLightColor(level, neighbour);

            poseStack.pushPose();
            poseStack.translate(0.5F, 0.5F, 0.5F);
            // Local +Z now points out of the face, local +X to the viewer's right and +Y up.
            poseStack.mulPose(Axis.YP.rotationDegrees(-direction.toYRot()));
            poseStack.translate(0.0F, 0.0F, HALF + FACE_OFFSET);

            PoseStack.Pose pose = poseStack.last();
            Matrix4f matrix = pose.pose();
            vertex(consumer, pose, matrix, -HALF, HALF, 0.0F, 0.0F, light);
            vertex(consumer, pose, matrix, HALF, HALF, 1.0F, 0.0F, light);
            vertex(consumer, pose, matrix, HALF, -HALF, 1.0F, 1.0F, light);
            vertex(consumer, pose, matrix, -HALF, -HALF, 0.0F, 1.0F, light);

            poseStack.popPose();
        }
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Matrix4f matrix,
                               float x, float y, float u, float v, int light) {
        consumer.addVertex(matrix, x, y, 0.0F)
            .setColor(255, 255, 255, 255)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(pose, 0.0F, 0.0F, 1.0F);
    }
}
