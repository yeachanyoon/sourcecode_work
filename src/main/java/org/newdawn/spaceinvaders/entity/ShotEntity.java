package org.newdawn.spaceinvaders.entity;

import org.newdawn.spaceinvaders.Game;

/**
 * 플레이어가 쏘는 총알 엔티티
 */
public class ShotEntity extends Entity {
	private double moveSpeed = -300; // 위로 올라감
	private Game game;
	private boolean used = false;

	public ShotEntity(Game game, String sprite, int x, int y) {
		super(sprite, x, y);
		this.game = game;
		dy = moveSpeed;
	}

	/** 이동 처리 */
	public void move(long delta) {
		super.move(delta);

		// 화면 위로 벗어나면 제거
		if (y < -100) {
			game.removeEntity(this);
		}
	}

	/** 충돌 처리 */
	public void collidedWith(Entity other) {
		if (used) return;

		// 🔹 에일리언과 충돌 시
		if (other instanceof AlienEntity) {
			game.removeEntity(this);
			game.removeEntity(other);
			used = true;

			int cx = other.getX() + other.getWidth() / 2;
			int cy = other.getY() + other.getHeight() / 2;
			game.notifyAlienKilledAt(cx, cy);
		}
	}

	/** 🔸 추상 메서드 구현 (Entity 상속 필수) */
	@Override
	public void doLogic() {
		// 총알은 별도의 논리 업데이트가 필요 없음
	}
}
