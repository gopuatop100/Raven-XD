package keystrokesmod.module.impl.world;

import keystrokesmod.Raven;
import keystrokesmod.event.*;
import keystrokesmod.mixins.impl.client.KeyBindingAccessor;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.other.RotationHandler;
import keystrokesmod.module.impl.other.SlotHandler;
import keystrokesmod.module.impl.render.HUD;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.ModeSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.module.setting.utils.ModeOnly;
import keystrokesmod.utility.*;
import keystrokesmod.utility.Timer;
import keystrokesmod.utility.render.RenderUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.*;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class Scaffold extends Module { // from b4 :)
    private final SliderSetting aimSpeed;
    private final SliderSetting motion;
    private final ModeSetting rotation;
    private final ButtonSetting moveFix;
    private final SliderSetting tellyStartTick;
    private final SliderSetting tellyStopTick;
    private final SliderSetting strafe;
    private final ModeSetting fastScaffold;
    private final ButtonSetting cancelSprint;
    private final ButtonSetting rayCast;
    private final ButtonSetting sneak;
    private final SliderSetting sneakEveryBlocks;
    private final SliderSetting sneakTime;
    private final ModeSetting precision;
    private final ButtonSetting autoSwap;
    private final ButtonSetting fastOnRMB;
    private final ButtonSetting highlightBlocks;
    private final ButtonSetting multiPlace;
    public final ButtonSetting safeWalk;
    private final ButtonSetting showBlockCount;
    private final ButtonSetting delayOnJump;
    private final ButtonSetting silentSwing;
    public final ButtonSetting tower;
    public final ButtonSetting fast;
    public final ButtonSetting sameY;
    public final ButtonSetting autoJump;

    public MovingObjectPosition placeBlock;
    private int lastSlot;
    private static final String[] rotationModes = new String[]{"None", "Backwards", "Strict", "Precise", "Telly", "Constant", "Snap"};
    private static final String[] fastScaffoldModes = new String[]{"Disabled", "Sprint", "Edge", "Jump A", "Jump B", "Jump C", "Float", "Side", "Legit", "GrimAC", "Sneak"};
    private static final String[] precisionModes = new String[]{"Very low", "Low", "Moderate", "High", "Very high"};
    public float placeYaw;
    public float placePitch;
    public int at;
    public int index;
    public boolean rmbDown;
    private double startPos = -1;
    private final Map<BlockPos, Timer> highlight = new HashMap<>();
    private boolean forceStrict;
    private boolean down;
    private boolean delay;
    private boolean place;
    private int add = 0;
    private int sameY$bridged = 1;
    private int sneak$bridged = 0;
    private boolean placedUp;
    private int offGroundTicks = 0;
    private boolean telly$noBlockPlace = false;
    public boolean tower$noBlockPlace = false;
    private Float lastYaw = null, lastPitch = null;
    public Scaffold() {
        super("Scaffold", category.world);
        this.registerSetting(aimSpeed = new SliderSetting("Aim speed", 20, 5, 20, 0.1));
        this.registerSetting(motion = new SliderSetting("Motion", 1.0, 0.5, 1.2, 0.01));
        this.registerSetting(rotation = new ModeSetting("Rotation", rotationModes, 1));
        this.registerSetting(moveFix = new ButtonSetting("MoveFix", false));
        this.registerSetting(tellyStartTick = new SliderSetting("Telly start", 3, 0, 11, 1, "tick", new ModeOnly(rotation, 4)));
        this.registerSetting(tellyStopTick = new SliderSetting("Telly stop", 8, 0, 11, 1, "tick", new ModeOnly(rotation, 4)));
        this.registerSetting(strafe = new SliderSetting("Strafe", 0, -45, 45, 5));
        this.registerSetting(fastScaffold = new ModeSetting("Fast scaffold", fastScaffoldModes, 0));
        this.registerSetting(precision = new ModeSetting("Precision", precisionModes, 4));
        this.registerSetting(cancelSprint = new ButtonSetting("Cancel sprint", false, new ModeOnly(fastScaffold, 0).reserve()));
        this.registerSetting(rayCast = new ButtonSetting("Ray cast", false));
        this.registerSetting(sneak = new ButtonSetting("Sneak", false));
        this.registerSetting(sneakEveryBlocks = new SliderSetting("Sneak every blocks", 1, 1, 10, 1, sneak::isToggled));
        this.registerSetting(sneakTime = new SliderSetting("Sneak time", 50, 0, 500, 10, "ms", sneak::isToggled));
        this.registerSetting(autoSwap = new ButtonSetting("AutoSwap", true));
        this.registerSetting(delayOnJump = new ButtonSetting("Delay on jump", true));
        this.registerSetting(fastOnRMB = new ButtonSetting("Fast on RMB", false));
        this.registerSetting(highlightBlocks = new ButtonSetting("Highlight blocks", true));
        this.registerSetting(multiPlace = new ButtonSetting("Multi-place", false));
        this.registerSetting(safeWalk = new ButtonSetting("Safewalk", true));
        this.registerSetting(showBlockCount = new ButtonSetting("Show block count", true));
        this.registerSetting(silentSwing = new ButtonSetting("Silent swing", false));
        this.registerSetting(tower = new ButtonSetting("Tower", false));
        this.registerSetting(fast = new ButtonSetting("Fast", false));
        this.registerSetting(sameY = new ButtonSetting("SameY", false));
        this.registerSetting(autoJump = new ButtonSetting("Auto jump", false));
    }

    public void onDisable() {
        placeBlock = null;
        if (lastSlot != -1) {
            SlotHandler.setCurrentSlot(lastSlot);
            lastSlot = -1;
        }
        delay = false;
        highlight.clear();
        at = index = 0;
        add = 0;
        startPos = -1;
        forceStrict = false;
        down = false;
        place = false;
        placedUp = false;
        sameY$bridged = 1;
        offGroundTicks = 0;
        telly$noBlockPlace = false;
        tower$noBlockPlace = false;
        lastYaw = lastPitch = null;
    }

    public void onEnable() {
        lastSlot = -1;
        startPos = mc.thePlayer.posY;
    }

    @SubscribeEvent
    public void onRotation(RotationEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }
        float yaw = event.getYaw();
        float pitch = event.getPitch();
        switch ((int) rotation.getInput()) {
            case 0:
                break;
            case 1:
                yaw = getYaw() + (float) strafe.getInput();
                pitch = 85;
                break;
            case 2:
                if (!forceStrict && MoveUtil.isMoving()) {
                    yaw = getYaw() + (float) strafe.getInput();
                    pitch = 85;
                    break;
                }
            case 3:
                yaw = placeYaw;
                pitch = placePitch;
                break;
            case 4:
                if (offGroundTicks >= tellyStartTick.getInput() && offGroundTicks < tellyStopTick.getInput() && placeBlock != null && MoveUtil.isMoving() && !Utils.jumpDown()) {
                    telly$noBlockPlace = true;
                    yaw = event.getYaw();
                    pitch = event.getPitch();
                } else {
                    yaw = placeYaw;
                    pitch = placePitch;
                    telly$noBlockPlace = false;
                }
                break;
            case 5:
                yaw = getYaw();
                pitch = placePitch;
                break;
            case 6:
                if (MoveUtil.isMoving()) {
                    mc.thePlayer.setSprinting(true);
                }
                if (!MoveUtil.isMoving()) {
                    yaw = placeYaw;
                } else if (place) {
                    yaw = forceStrict ? placeYaw : getYaw();
                } else {
                    yaw = (float) (event.getYaw() + (Math.random() - 0.5) * 0.940004);
                }
                pitch = placePitch;
                break;
        }
        boolean instant = aimSpeed.getInput() == aimSpeed.getMax();

        if (lastYaw == null || lastPitch == null) {
            lastYaw = event.getYaw();
            lastPitch = event.getPitch();
        }

        event.setYaw(lastYaw = instant ? yaw : AimSimulator.rotMove(yaw, lastYaw, (float) aimSpeed.getInput()));
        event.setPitch(lastPitch = instant ? pitch : AimSimulator.rotMove(pitch, lastPitch, (float) aimSpeed.getInput()));
        event.setMoveFix(moveFix.isToggled() ? RotationHandler.MoveFix.SILENT : RotationHandler.MoveFix.NONE);

        place = true;
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent event) {
        if (cancelSprint.isToggled()) {
            event.setSprinting(false);
        }

        if (fastScaffold.getInput() == 10) {
            if (Math.abs(MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw) -
                    MathHelper.wrapAngleTo180_float(RotationHandler.getRotationYaw())) > 100) {
                ((KeyBindingAccessor) mc.gameSettings.keyBindSprint).setPressed(false);
                mc.thePlayer.setSprinting(false);
            }
        }
    }

    @SubscribeEvent
    public void onJump(JumpEvent e) {
        delay = true;
    }

    @SubscribeEvent
    public void onMoveInput(@NotNull MoveInputEvent event) {
        if (fastScaffold.getInput() == 10) {
            event.setSneak(true);
            event.setSneakSlowDownMultiplier(1);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPreUpdate(PreUpdateEvent e) { // place here
        if (mc.thePlayer.onGround) {
            offGroundTicks = 0;
        } else {
            offGroundTicks++;
        }
        if ((rotation.getInput() == 4 || autoJump.isToggled()) && mc.thePlayer.onGround && MoveUtil.isMoving() && !Utils.jumpDown()) {
            mc.thePlayer.jump();
        }

        if (fast.isToggled() && mc.gameSettings.keyBindJump.isKeyDown()) {
            if (mc.gameSettings.keyBindForward.isKeyDown() && mc.thePlayer.onGround) {
                if (!isDiagonal() && !(mc.gameSettings.keyBindRight.isKeyDown() || mc.gameSettings.keyBindLeft.isKeyDown())) {
                    MoveUtil.strafe(0.5);
                    mc.thePlayer.setSprinting(false);
                    mc.thePlayer.jump();
                }
            }
        }

        if (fastScaffold.getInput() == 7 && !Utils.jumpDown() && sameY$bridged != 0 && sameY$bridged % 2 == 0 && placeBlock != null && !Utils.jumpDown()) {
            List<BlockPos> possible = new ArrayList<>(Arrays.asList(
                    placeBlock.getBlockPos().west(),
                    placeBlock.getBlockPos().east(),
                    placeBlock.getBlockPos().north(),
                    placeBlock.getBlockPos().south()
            ));

            for (BlockPos pos : possible) {
                if (!BlockUtils.replaceable(pos)) continue;

                Optional<Triple<BlockPos, EnumFacing, keystrokesmod.script.classes.Vec3>> placeSide = RotationUtils.getPlaceSide(pos);
                if (!placeSide.isPresent()) continue;

                place(new MovingObjectPosition(MovingObjectPosition.MovingObjectType.BLOCK,
                                placeSide.get().getRight().toVec3(),
                                placeSide.get().getMiddle(),
                                placeSide.get().getLeft())
                        , true);
                sameY$bridged = 0;
                break;
            }
        }

        if (delay && delayOnJump.isToggled()) {
            delay = false;
            return;
        }
        final ItemStack heldItem = SlotHandler.getHeldItem();
        if (!autoSwap.isToggled() || getSlot() == -1) {
            if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock)) {
                return;
            }
        }
        if (keepYPosition() && !down) {
            startPos = Math.floor(mc.thePlayer.posY);
            down = true;
        }
        else if (!keepYPosition()) {
            down = false;
            placedUp = false;
        }
        if (keepYPosition() && (fastScaffold.getInput() == 3 || fastScaffold.getInput() == 4 || fastScaffold.getInput() == 5) && mc.thePlayer.onGround) {
            mc.thePlayer.jump();
            add = 0;
            if (Math.floor(mc.thePlayer.posY) == Math.floor(startPos) && fastScaffold.getInput() == 5) {
                placedUp = false;
            }
        }
        double original = startPos;
        if (fastScaffold.getInput() == 3) {
            if (groundDistance() >= 2 && add == 0) {
                original++;
                add++;
            }
        }
        else if (fastScaffold.getInput() == 4 || fastScaffold.getInput() == 5) {
            if (groundDistance() > 0 && mc.thePlayer.posY >= Math.floor(mc.thePlayer.posY) && mc.thePlayer.fallDistance > 0 && ((!placedUp || isDiagonal()) || fastScaffold.getInput() == 4)) {
                original++;
            }
        }
        Vec3 targetVec3 = getPlacePossibility(0, original);
        if (targetVec3 == null) {
            return;
        }
        BlockPos targetPos = new BlockPos(targetVec3.xCoord, targetVec3.yCoord, targetVec3.zCoord);

        if (mc.thePlayer.onGround && Utils.isMoving() && motion.getInput() != 1.0) {
            Utils.setSpeed(Utils.getHorizontalSpeed() * motion.getInput());
        }
        int slot = getSlot();
        if (slot == -1) {
            return;
        }
        if (lastSlot == -1) {
            lastSlot = SlotHandler.getCurrentSlot();
        }
        SlotHandler.setCurrentSlot(slot);
        if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock)) {
            return;
        }
        MovingObjectPosition rayCasted = null;
        float searchYaw = 25;
        switch ((int) precision.getInput()) {
            case 0:
                searchYaw = 35;
                break;
            case 1:
                searchYaw = 30;
                break;
            case 2:
                break;
            case 3:
                searchYaw = 15;
                break;
            case 4:
                searchYaw = 5;
                break;
        }
        EnumFacingOffset enumFacing = getEnumFacing(targetVec3);
        if (enumFacing == null) {
            return;
        }
        targetPos = targetPos.add(enumFacing.getOffset().xCoord, enumFacing.getOffset().yCoord, enumFacing.getOffset().zCoord);
        float[] targetRotation = RotationUtils.getRotations(targetPos);
        float[] searchPitch = new float[]{78, 12};
        for (int i = 0; i < 2; i++) {
            if (i == 1 && Utils.overPlaceable(-1)) {
                searchYaw = 180;
                searchPitch = new float[]{65, 25};
            }
            else if (i == 1) {
                break;
            }
            for (float checkYaw : generateSearchSequence(searchYaw)) {
                float playerYaw = isDiagonal() ? getYaw() : targetRotation[0];
                float fixedYaw = (float) (playerYaw - checkYaw + getRandom());
                double deltaYaw = Math.abs(playerYaw - fixedYaw);
                if (i == 1 && (inBetween(75, 95, (float) deltaYaw)) || deltaYaw > 500) {
                    continue;
                }
                for (float checkPitch : generateSearchSequence(searchPitch[1])) {
                    float fixedPitch = RotationUtils.clampTo90((float) (targetRotation[1] + checkPitch + getRandom()));
                    MovingObjectPosition raycast = RotationUtils.rayTraceCustom(mc.playerController.getBlockReachDistance(), fixedYaw, fixedPitch);
                    if (raycast != null) {
                        if (raycast.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                            if (raycast.getBlockPos().equals(targetPos) && raycast.sideHit == enumFacing.getEnumFacing()) {
                                if (rayCasted == null || !BlockUtils.isSamePos(raycast.getBlockPos(), rayCasted.getBlockPos())) {
                                    if (((ItemBlock) heldItem.getItem()).canPlaceBlockOnSide(mc.theWorld, raycast.getBlockPos(), raycast.sideHit, mc.thePlayer, heldItem)) {
                                        if (rayCasted == null) {
                                            forceStrict = (forceStrict(checkYaw)) && i == 1;
                                            rayCasted = raycast;
                                            placeYaw = fixedYaw;
                                            placePitch = fixedPitch;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (rayCasted != null) {
                break;
            }
        }
        if (rayCasted != null && (place || rotation.getInput() == 0)) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
            placeBlock = rayCasted;
            if (multiPlace.isToggled()) {
                place(placeBlock, true);
            }
            place(placeBlock, false);
            sameY$bridged++;
            place = false;
            if (placeBlock.sideHit == EnumFacing.UP && keepYPosition()) {
                placedUp = true;
            }
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent ev) {
        if (!Utils.nullCheck() || !showBlockCount.isToggled()) {
            return;
        }
        if (ev.phase == TickEvent.Phase.END) {
            if (mc.currentScreen != null) {
                return;
            }
            final ScaledResolution scaledResolution = new ScaledResolution(mc);
            int blocks = totalBlocks();
            String color = "§";
            if (blocks <= 5) {
                color += "c";
            }
            else if (blocks <= 15) {
                color += "6";
            }
            else if (blocks <= 25) {
                color += "e";
            }
            else {
                color = "";
            }
            mc.fontRendererObj.drawStringWithShadow(color + blocks + " §rblock" + (blocks == 1 ? "" : "s"), scaledResolution.getScaledWidth()/2 + 8, scaledResolution.getScaledHeight()/2 + 4, -1);
        }
    }

    public Vec3 getPlacePossibility(double offsetY, double original) { // rise
        List<Vec3> possibilities = new ArrayList<>();
        int range = 5;
        for (int x = -range; x <= range; ++x) {
            for (int y = -range; y <= range; ++y) {
                for (int z = -range; z <= range; ++z) {
                    final Block block = blockRelativeToPlayer(x, y, z);
                    if (!block.getMaterial().isReplaceable()) {
                        for (int x2 = -1; x2 <= 1; x2 += 2) {
                            possibilities.add(new Vec3(mc.thePlayer.posX + x + x2, mc.thePlayer.posY + y, mc.thePlayer.posZ + z));
                        }
                        for (int y2 = -1; y2 <= 1; y2 += 2) {
                            possibilities.add(new Vec3(mc.thePlayer.posX + x, mc.thePlayer.posY + y + y2, mc.thePlayer.posZ + z));
                        }
                        for (int z2 = -1; z2 <= 1; z2 += 2) {
                            possibilities.add(new Vec3(mc.thePlayer.posX + x, mc.thePlayer.posY + y, mc.thePlayer.posZ + z + z2));
                        }
                    }
                }
            }
        }

        possibilities.removeIf(vec3 -> mc.thePlayer.getDistance(vec3.xCoord, vec3.yCoord, vec3.zCoord) > 5);

        if (possibilities.isEmpty()) {
            return null;
        }
        possibilities.sort(Comparator.comparingDouble(vec3 -> {
            final double d0 = (mc.thePlayer.posX) - vec3.xCoord;
            final double d1 = ((keepYPosition() ? original : mc.thePlayer.posY) - 1 + offsetY) - vec3.yCoord;
            final double d2 = (mc.thePlayer.posZ) - vec3.zCoord;
            return MathHelper.sqrt_double(d0 * d0 + d1 * d1 + d2 * d2);
        }));

        return possibilities.get(0);
    }

    public Block blockRelativeToPlayer(final double offsetX, final double offsetY, final double offsetZ) {
        return mc.theWorld.getBlockState(new BlockPos(mc.thePlayer).add(offsetX, offsetY, offsetZ)).getBlock();
    }

    public float[] generateSearchSequence(float value) {
        int length = (int) value * 2;
        float[] sequence = new float[length + 1];

        int index = 0;
        sequence[index++] = 0;

        for (int i = 1; i <= value; i++) {
            sequence[index++] = i;
            sequence[index++] = -i;
        }

        return sequence;
    }

    @SubscribeEvent
    public void onMouse(@NotNull MouseEvent mouseEvent) {
        if (mouseEvent.button == 1) {
            rmbDown = mouseEvent.buttonstate;
            if (placeBlock != null && rmbDown) {
                mouseEvent.setCanceled(true);
            }
        }
    }

    public boolean stopFastPlace() {
        return this.isEnabled() && placeBlock != null;
    }

    private boolean isDiagonal() {
        float yaw = ((mc.thePlayer.rotationYaw % 360) + 360) % 360 > 180 ? ((mc.thePlayer.rotationYaw % 360) + 360) % 360 - 360 : ((mc.thePlayer.rotationYaw % 360) + 360) % 360;
        return (yaw >= -170 && yaw <= 170) && !(yaw >= -10 && yaw <= 10) && !(yaw >= 80 && yaw <= 100) && !(yaw >= -100 && yaw <= -80) || Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode()) || Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode());
    }

    public double groundDistance() {
        for (int i = 1; i <= 20; i++) {
            if (!mc.thePlayer.onGround && !(BlockUtils.getBlock(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - (i / 10), mc.thePlayer.posZ)) instanceof BlockAir)) {
                return (i / 10);
            }
        }
        return -1;
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (!Utils.nullCheck() || !highlightBlocks.isToggled() || highlight.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<BlockPos, Timer>> iterator = highlight.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Timer> entry = iterator.next();
            if (entry.getValue() == null) {
                entry.setValue(new Timer(750));
                entry.getValue().start();
            }
            int alpha = entry.getValue() == null ? 210 : 210 - entry.getValue().getValueInt(0, 210, 1);
            if (alpha == 0) {
                iterator.remove();
                continue;
            }
            RenderUtils.renderBlock(entry.getKey(), Utils.merge(Theme.getGradient((int) HUD.theme.getInput(), 0), alpha), true, false);
        }
    }

    public static boolean sprint() {
        if (ModuleManager.scaffold.isEnabled()
                && ModuleManager.scaffold.fastScaffold.getInput() > 0
                && ModuleManager.scaffold.placeBlock != null
                && (!ModuleManager.scaffold.fastOnRMB.isToggled() || Mouse.isButtonDown(1))) {
            switch ((int) ModuleManager.scaffold.fastScaffold.getInput()) {
                case 1:
                case 7:
                case 9:
                case 10:
                    return true;
                case 2:
                    return Utils.onEdge();
                case 3:
                case 4:
                case 5:
                case 6:
                    return ModuleManager.scaffold.keepYPosition();
                case 8:
                    return Math.abs(MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw) - MathHelper.wrapAngleTo180_float(RotationHandler.getRotationYaw())) <= 45;
            }
        }
        return false;
    }

    private boolean forceStrict(float value) {
        return (inBetween(-170, -105, value) || inBetween(-80, 80, value) || inBetween(98, 170, value)) && !inBetween(-10, 10, value);
    }

    private boolean keepYPosition() {
        boolean sameYSca = fastScaffold.getInput() == 4 || fastScaffold.getInput() == 3 || fastScaffold.getInput() == 5 || fastScaffold.getInput() == 6;
        return this.isEnabled() && Utils.keysDown() && (sameYSca || (sameY.isToggled() && !Utils.jumpDown())) && (!Utils.jumpDown() || fastScaffold.getInput() == 6) && (!fastOnRMB.isToggled() || Mouse.isButtonDown(1));
    }

    public boolean safewalk() {
        return this.isEnabled() && safeWalk.isToggled() && (!keepYPosition() || fastScaffold.getInput() == 3);
    }

    public boolean stopRotation() {
        return this.isEnabled() && (rotation.getInput() <= 1 || (rotation.getInput() == 2 && placeBlock != null));
    }

    private boolean inBetween(float min, float max, float value) {
        return value >= min && value <= max;
    }

    private double getRandom() {
        return Utils.randomizeInt(-90, 90) / 100.0;
    }

    public float getYaw() {
        float yaw = 0.0f;
        double moveForward = mc.thePlayer.movementInput.moveForward;
        double moveStrafe = mc.thePlayer.movementInput.moveStrafe;
        if (moveForward == 0.0) {
            if (moveStrafe == 0.0) {
                yaw = 180.0f;
            }
            else if (moveStrafe > 0.0) {
                yaw = 90.0f;
            }
            else if (moveStrafe < 0.0) {
                yaw = -90.0f;
            }
        }
        else if (moveForward > 0.0) {
            if (moveStrafe == 0.0) {
                yaw = 180.0f;
            }
            else if (moveStrafe > 0.0) {
                yaw = 135.0f;
            }
            else if (moveStrafe < 0.0) {
                yaw = -135.0f;
            }
        }
        else if (moveForward < 0.0) {
            if (moveStrafe == 0.0) {
                yaw = 0.0f;
            }
            else if (moveStrafe > 0.0) {
                yaw = 45.0f;
            }
            else if (moveStrafe < 0.0) {
                yaw = -45.0f;
            }
        }
        return mc.thePlayer.rotationYaw + yaw;
    }

    private @Nullable EnumFacingOffset getEnumFacing(final Vec3 position) {
        for (int x2 = -1; x2 <= 1; x2 += 2) {
            if (!BlockUtils.getBlock(position.xCoord + x2, position.yCoord, position.zCoord).getMaterial().isReplaceable()) {
                if (x2 > 0) {
                    return new EnumFacingOffset(EnumFacing.WEST, new Vec3(x2, 0, 0));
                } else {
                    return new EnumFacingOffset(EnumFacing.EAST, new Vec3(x2, 0, 0));
                }
            }
        }

        for (int y2 = -1; y2 <= 1; y2 += 2) {
            if (!BlockUtils.getBlock(position.xCoord, position.yCoord + y2, position.zCoord).getMaterial().isReplaceable()) {
                if (y2 < 0) {
                    return new EnumFacingOffset(EnumFacing.UP, new Vec3(0, y2, 0));
                }
            }
        }

        for (int z2 = -1; z2 <= 1; z2 += 2) {
            if (!BlockUtils.getBlock(position.xCoord, position.yCoord, position.zCoord + z2).getMaterial().isReplaceable()) {
                if (z2 < 0) {
                    return new EnumFacingOffset(EnumFacing.SOUTH, new Vec3(0, 0, z2));
                } else {
                    return new EnumFacingOffset(EnumFacing.NORTH, new Vec3(0, 0, z2));
                }
            }
        }

        return null;
    }

    public void place(MovingObjectPosition block, boolean extra) {
        if (rotation.getInput() == 4 && telly$noBlockPlace) return;
        if (tower$noBlockPlace) {
            tower$noBlockPlace = false;
            return;
        }

        if (sneak.isToggled()) {
            if (sneak$bridged >= sneakEveryBlocks.getInput()) {
                sneak$bridged = 0;
                ((KeyBindingAccessor) mc.gameSettings.keyBindSneak).setPressed(true);
                Raven.getExecutor().schedule(() -> ((KeyBindingAccessor) mc.gameSettings.keyBindSneak).setPressed(Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode())), (long) sneakTime.getInput(), TimeUnit.MILLISECONDS);
            }
        }

        ItemStack heldItem = SlotHandler.getHeldItem();
        if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock)) {
            return;
        }

        if (rayCast.isToggled()) {
            MovingObjectPosition hitResult = RotationUtils.rayCast(4.5, lastYaw, lastPitch);
            if (hitResult != null && hitResult.getBlockPos().equals(block.getBlockPos())) {
                block.hitVec = hitResult.hitVec;
            } else {
                return;
            }
        }

        if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, heldItem, block.getBlockPos(), block.sideHit, block.hitVec)) {
            sneak$bridged++;
            if (silentSwing.isToggled()) {
                mc.thePlayer.sendQueue.addToSendQueue(new C0APacketAnimation());
            }
            else {
                mc.thePlayer.swingItem();
                mc.getItemRenderer().resetEquippedProgress();
            }
            if (!extra) {
                highlight.put(block.getBlockPos().offset(block.sideHit), null);
            }
        }
    }

    public static int getSlot() {
        int slot = -1;
        int highestStack = -1;
        for (int i = 0; i < 9; ++i) {
            final ItemStack itemStack = mc.thePlayer.inventory.mainInventory[i];
            if (itemStack != null && itemStack.getItem() instanceof ItemBlock && ContainerUtils.canBePlaced((ItemBlock) itemStack.getItem()) && itemStack.stackSize > 0) {
                if (mc.thePlayer.inventory.mainInventory[i].stackSize > highestStack) {
                    highestStack = mc.thePlayer.inventory.mainInventory[i].stackSize;
                    slot = i;
                }
            }
        }
        return slot;
    }

    public int totalBlocks() {
        int totalBlocks = 0;
        for (int i = 0; i < 9; ++i) {
            final ItemStack stack = mc.thePlayer.inventory.mainInventory[i];
            if (stack != null && stack.getItem() instanceof ItemBlock && ContainerUtils.canBePlaced((ItemBlock) stack.getItem()) && stack.stackSize > 0) {
                totalBlocks += stack.stackSize;
            }
        }
        return totalBlocks;
    }

    static class EnumFacingOffset {
        EnumFacing enumFacing;
        Vec3 offset;

        EnumFacingOffset(EnumFacing enumFacing, Vec3 offset) {
            this.enumFacing = enumFacing;
            this.offset = offset;
        }

        EnumFacing getEnumFacing() {
            return enumFacing;
        }

        Vec3 getOffset() {
            return offset;
        }
    }

    @Override
    public String getInfo() {
        return fastScaffoldModes[(int) fastScaffold.getInput()];
    }
}