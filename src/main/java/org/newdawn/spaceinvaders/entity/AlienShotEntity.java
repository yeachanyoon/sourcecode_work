// AlienShotEntity.java
package org.newdawn.spaceinvaders.entity;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import org.newdawn.spaceinvaders.Game;

public class AlienShotEntity extends Entity {
    private final Game game;
    private double bulletSpeed = 400; // 아래로 이동

    public AlienShotEntity(Game game, String sprite, int x, int y) {
        super(sprite, x, y);   // 예: "sprites/shot.gif"
        this.game = game;
        this.dy = bulletSpeed;
    }

    public void move(long delta) {
        super.move(delta);
        if (y > 700) {
            game.removeEntity(this);
        }
    }

    public void doLogic() { /* no-op */ }

    public boolean collidesWith(Entity other) {
        return super.collidesWith(other);
    }

    public void collidedWith(Entity other) {
        if (other instanceof ShipEntity) {
            game.removeEntity(this);
            if (game.isPlayerInvincible()) return;
            game.playerHit();
        }
    }

    // ⬇️ 여기 추가: 180도 회전해서 그리기
    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        AffineTransform old = g2.getTransform();

        int w = sprite.getWidth();
        int h = sprite.getHeight();

        // 총알 중심 기준 회전
        g2.translate((int)x + w / 2.0, (int)y + h / 2.0);
        g2.rotate(Math.PI); // 180도
        // 회전된 좌표계에서 좌상단 보정
        sprite.draw(g2, -w / 2, -h / 2);

        g2.setTransform(old);
    }
}

