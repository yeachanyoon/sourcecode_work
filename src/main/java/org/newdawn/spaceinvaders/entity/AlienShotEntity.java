package org.newdawn.spaceinvaders.entity;

import org.newdawn.spaceinvaders.Game;

/** 에일리언이 아래로 쏘는 탄 */
public class AlienShotEntity extends Entity {
    private static final double SPEED = 200; // px/sec (난이도 조절용)
    private final Game game;

    public AlienShotEntity(Game game, String sprite, int x, int y) {
        super(sprite, x, y);
        this.game = game;
        this.dy = SPEED; // 아래로 진행
    }

    @Override
    public void move(long delta) {
        super.move(delta);
        // 화면 아래로 나가면 정리
        if (y > Game.VIRTUAL_HEIGHT + 100) {
            game.removeEntity(this);
        }
    }

    @Override
    public void collidedWith(Entity other) {
        // Ship과의 충돌은 ShipEntity 쪽에서 처리(사망). 여긴 아무 것도 안 해도 됨.
        // 혹시 우주선과 부딪히면 즉시 탄 제거:
        if (other instanceof ShipEntity) {
            game.removeEntity(this);
        }
    }
}