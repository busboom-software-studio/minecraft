package com.nolan.nolanmod.client.plane;

import com.nolan.nolanmod.plane.PlaneType;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** The handful of values the renderer needs, snapshotted once per frame. */
public class PlaneRenderState extends EntityRenderState {
	public PlaneType type = PlaneType.PROP;
	public float yRot;
	public float xRot;
	public float roll;
	public float propellerAngle;
	public float throttle;
}
