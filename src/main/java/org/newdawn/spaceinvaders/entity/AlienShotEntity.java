package org.newdawn.spaceinvaders.entity;

import org.newdawn.spaceinvaders.Game;

//에일리언이 총알을 쏨
public class AlienShotEntity extends Entity {
    private final Game game;
    private double bulletSpeed = 400; //총알 스피드

    public AlienShotEntity(Game game, String sprite, int x, int y) {
        super(sprite, x, y);
        this.game = game;
        this.dy = bulletSpeed;
    }

    @Override
    public void move(long delta) {
        super.move(delta);
        // 화면 아래로 벗어나면 제거
        if (y > Game.VIRTUAL_HEIGHT + 100) {
            game.removeEntity(this);
        }
    }

    @Override
    public void collidedWith(Entity other) {

        if (other instanceof ShipEntity) {
            game.removeEntity(this);
            if (game.isPlayerInvincible()) return;
            game.playerHit();
        }
    }
}