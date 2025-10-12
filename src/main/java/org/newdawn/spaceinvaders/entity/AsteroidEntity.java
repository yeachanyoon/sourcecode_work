package org.newdawn.spaceinvaders.entity;

import org.newdawn.spaceinvaders.Game;

public class AsteroidEntity extends Entity{
    private final Game game;

    public AsteroidEntity(Game game, int x, int y, double fallSpeed) {
        super("sprites/asteroid.png", x, y);
        this.game = game;
        this.dy = fallSpeed;
    }

    @Override
    public void move(long delta) {
        super.move(delta);
        // 화면 아래로 벗어나면 제거
        if (y > Game.VIRTUAL_HEIGHT) {
            game.removeEntity(this);
        }
    }

    @Override
    public void collidedWith(Entity other) {
        if (other instanceof ShipEntity) {
            game.loseHeart();
            game.removeEntity(this);
        }
    }
}
