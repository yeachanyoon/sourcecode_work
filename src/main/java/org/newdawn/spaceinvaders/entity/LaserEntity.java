package org.newdawn.spaceinvaders.entity;

import org.newdawn.spaceinvaders.Game;

import java.awt.*;

public class LaserEntity extends Entity{

    private final Game game;
    private final int laserDamage;
    private final int w, h;

    public LaserEntity(Game game, int x, int y, int w, int h, int laserDamage) {
        super("sprites/shot.gif", x, y); // 스프라이트는 안 씀
        this.game = game;
        this.laserDamage = laserDamage;
        this.w = w; this.h = h;
    }
    public int getLaserDamage() { return laserDamage; }

    @Override public void draw(Graphics g) { }

    @Override
    public boolean collidesWith(Entity other) { //충돌 판정
        Rectangle me  = new Rectangle(getX(), getY(), w, h);
        Rectangle him = new Rectangle(other.getX(), other.getY(), other.getWidth(), other.getHeight());
        return me.intersects(him);
    }
    @Override public void move(long delta) { }

    @Override public void collidedWith(Entity other) {
        if (other instanceof AlienEntity) {
            game.removeEntity(other);
            game.notifyAlienKilled();
        }
    }
}


