package org.example.Kangnaixi.tiandao.spell.node.projectile;

public class ProjectileTemplate {
    private final String id;
    private final double speed;
    private final double gravity;
    private final int lifetime;
    private final int maxTargets;
    private final boolean pierceBlocks;
    private final String trailParticle;

    public ProjectileTemplate(String id,
                              double speed,
                              double gravity,
                              int lifetime,
                              int maxTargets,
                              boolean pierceBlocks,
                              String trailParticle) {
        this.id = id;
        this.speed = speed;
        this.gravity = gravity;
        this.lifetime = lifetime;
        this.maxTargets = maxTargets;
        this.pierceBlocks = pierceBlocks;
        this.trailParticle = trailParticle;
    }

    public String getId() {
        return id;
    }

    public double getSpeed() {
        return speed;
    }

    public double getGravity() {
        return gravity;
    }

    public int getLifetime() {
        return lifetime;
    }

    public int getMaxTargets() {
        return maxTargets;
    }

    public boolean isPierceBlocks() {
        return pierceBlocks;
    }

    public String getTrailParticle() {
        return trailParticle;
    }
}

