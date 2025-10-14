package org.newdawn.spaceinvaders.entity;

import org.newdawn.spaceinvaders.Game;

public class AsteroidEntity extends Entity {
    private final Game game;
    private final double fallSpeed;

    public AsteroidEntity(Game game, int x, int y, double fallSpeed) {
        super("sprites/asteroid.png", x, y); // 프로젝트 경로에 맞게 유지
        this.game = game;
        this.fallSpeed = fallSpeed;
        this.dy = fallSpeed;
    }

    @Override
    public void move(long delta) {
        // 직하강
        y += (int) Math.round((fallSpeed * delta) / 1000.0);
        if (y > Game.VIRTUAL_HEIGHT + 64) {
            game.removeEntity(this);
        }
    }

    @Override
    public void doLogic() {
        // 유성은 별도 로직 없음
    }

    @Override
    public void collidedWith(Entity other) {
        // 🔒 플레이어 총알은 무시: 총알로는 유성 파괴 불가
        if (other instanceof ShotEntity) {
            return;
        }

        // (참고) 레이저/폭탄은 Game.activateLaserAt / activateBombAt 에서 제거 처리하므로
        // 여기서 LaserEntity/BombEntity와의 충돌로 제거할 필요 없음.
        // => 충돌은 무시

        // 배랑 부딪히면 배는 피해, 유성은 제거 (원하면 유지/조정)
        if (other instanceof ShipEntity) {
            game.playerHit();
            game.removeEntity(this);
        }
    }
}
