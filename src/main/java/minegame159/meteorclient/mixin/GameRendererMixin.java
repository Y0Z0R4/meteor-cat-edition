package minegame159.meteorclient.mixin;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.events.RenderEvent;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.misc.UnfocusedCPU;
import minegame159.meteorclient.modules.player.LiquidInteract;
import minegame159.meteorclient.modules.player.NoMiningTrace;
import minegame159.meteorclient.modules.render.NoRender;
import minegame159.meteorclient.rendering.Matrices;
import minegame159.meteorclient.rendering.Renderer;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow @Final private MinecraftClient client;

    @Shadow @Final private Camera camera;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(float tickDelta, long startTime, boolean tick, CallbackInfo info) {
        if (ModuleManager.INSTANCE.isActive(UnfocusedCPU.class) && !client.isWindowFocused()) info.cancel();
    }

    @Inject(at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", args = { "ldc=hand" }), method = "renderCenter")
    public void onRenderCenter(float tickDelta, long endTime, CallbackInfo info) {
        if (!Utils.canUpdate()) return;

        client.getProfiler().swap("meteor-client_render");

        Matrices.begin();

        RenderEvent event = EventStore.renderEvent(tickDelta, camera.getPos().x, camera.getPos().y, camera.getPos().z);

        Renderer.begin(event);
        MeteorClient.EVENT_BUS.post(event);
        Renderer.end();
    }

    @Inject(method = "updateTargetedEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ProjectileUtil;rayTrace(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;D)Lnet/minecraft/util/hit/EntityHitResult;"), cancellable = true)
    private void onUpdateTargetedEntity(float tickDelta, CallbackInfo info) {
        if (ModuleManager.INSTANCE.get(NoMiningTrace.class).canWork() && client.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            client.getProfiler().pop();
            info.cancel();
        }
    }

    @Redirect(method = "updateTargetedEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;rayTrace(DFZ)Lnet/minecraft/util/hit/HitResult;"))
    private HitResult updateTargetedEntityEntityRayTraceProxy(Entity entity, double maxDistance, float tickDelta, boolean includeFluids) {
        if (ModuleManager.INSTANCE.isActive(LiquidInteract.class)) return entity.rayTrace(maxDistance, tickDelta, true);
        return entity.rayTrace(maxDistance, tickDelta, includeFluids);
    }

    @Inject(method = "bobViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void onBobViewWhenHurt(float tickDelta, CallbackInfo info) {
        if (ModuleManager.INSTANCE.get(NoRender.class).noHurtCam()) info.cancel();
    }

    @Inject(method = "renderWeather", at = @At("HEAD"), cancellable = true)
    private void onRenderWeather(float f, CallbackInfo info) {
        if (ModuleManager.INSTANCE.get(NoRender.class).noWeather()) info.cancel();
    }

    @Inject(method = "renderRain", at = @At("HEAD"), cancellable = true)
    private void onRenderRain(CallbackInfo info) {
        if (ModuleManager.INSTANCE.get(NoRender.class).noWeather()) info.cancel();
    }

    @Inject(method = "showFloatingItem", at = @At("HEAD"), cancellable = true)
    private void onShowFloatingItem(ItemStack floatingItem, CallbackInfo info) {
        if (floatingItem.getItem() == Items.TOTEM_OF_UNDYING && ModuleManager.INSTANCE.get(NoRender.class).noTotem()) {
            info.cancel();
        }
    }

    @Redirect(method = "applyCameraTransformations", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerp(FFF)F"))
    private float applyCameraTransformationsMathHelperLerpProxy(float delta, float first, float second) {
        if (ModuleManager.INSTANCE.get(NoRender.class).noNausea()) return 0;
        return MathHelper.lerp(delta, first, second);
    }
}
