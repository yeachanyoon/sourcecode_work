package org.newdawn.spaceinvaders.entity;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.SystemTimer;

//폭탄 아이템 추가
public class BombEntity extends Entity {
    private final Game game;
    private final int radius, baseDamage;
    private double moveSpeed;
    private long  fuseTime;
    private long  bornAt;

    public BombEntity(Game game, String sprite, int x, int y,
                      int radius, int baseDamage,
                      double moveSpeed, long fuseTime) {
        super(sprite, x, y);
        this.game = game;
        this.radius = radius;
        this.baseDamage = baseDamage;
        this.moveSpeed = moveSpeed;
        this.fuseTime    = fuseTime;
        this.dy = moveSpeed;
        this.bornAt = SystemTimer.getTime();
    }

    @Override
    public void move(long delta) {
        super.move(delta);
        if (SystemTimer.getTime() - bornAt >= fuseTime) {
            explode();
        }
    }

    private void explode() {
        int centerX = getX() + getWidth()/2;
        int centerY = getY() + getHeight()/2;
        game.applyExplosion(centerX, centerY, radius, baseDamage);
        game.addExplosionEffect(centerX, centerY);
        game.removeEntity(this);
    }

    @Override public void collidedWith(Entity other) {
        if (other instanceof AlienEntity || other instanceof BossEntity) {
            explode();
        }
    }
}
