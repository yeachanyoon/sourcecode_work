package org.newdawn.spaceinvaders.entity;

import java.awt.Graphics;
import org.newdawn.spaceinvaders.Game;

public class ItemBoxEntity extends Entity{
    private final Game game;

    public ItemBoxEntity(Game game, int x, int y) {
        super("sprites/itemBox.png", x, y); // 아이템 박스 스프라이트(임의)
        this.game = game;
    }

    @Override
    public void move(long delta) {
    }

    @Override
    public void draw(Graphics g) {
        super.draw(g); // 스프라이트 그리기
    }

    @Override
    public void collidedWith(Entity other) {
        if (other instanceof ShipEntity) {
            if (Math.random() < 0.5) {
                game.activateLaser(1000);
            } else {
                game.throwBomb(300);
            }
            game.removeEntity(this);
        }
    }
}
