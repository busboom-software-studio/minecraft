package com.nolan.nolanmod.client.mech;

import com.nolan.nolanmod.mech.MechType;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** The handful of values the renderer needs, snapshotted once per frame. */
public class MechRenderState extends EntityRenderState {
	public MechType type = MechType.SCOUT;
	public float yRot;
	public float headPitch;
	public float walkPhase;
	public float stride;
}
