package org.newdawn.spaceinvaders.entity;

import java.awt.Graphics;
import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.SystemTimer;

public class ExplosionEntity extends Entity {

    private final Game game;
    private long lifeTime = 3000;
    private long bornAt;

    public ExplosionEntity(Game game, int x, int y) {
        super("sprites/explosion.png", x, y);
        this.game = game;
        this.bornAt = SystemTimer.getTime();
    }

    @Override
    public void move(long delta) {
        if (SystemTimer.getTime() - bornAt >= lifeTime) {
            game.removeEntity(this);
        }
    }

    @Override
    public void draw(Graphics g) {
        super.draw(g);
    }

    @Override
    public void collidedWith(Entity other) {

    }
}
