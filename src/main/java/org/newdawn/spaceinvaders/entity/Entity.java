package org.newdawn.spaceinvaders.entity;

import java.awt.Graphics;
import java.awt.Rectangle;

import org.newdawn.spaceinvaders.Sprite;
import org.newdawn.spaceinvaders.SpriteStore;

/**
 * 모든 게임 오브젝트의 기본 엔티티 클래스.
 * - 위치/이동/그리기/충돌처리의 공통 로직을 제공
 * - 하위 클래스는 doLogic(), collidedWith()를 구현해야 함
 */
public abstract class Entity {
	/** 현재 스프라이트 */
	protected Sprite sprite;

	/** 위치 (더 부드러운 이동을 위해 double 사용) */
	protected double x;
	protected double y;

	/** 속도 (px/sec) */
	protected double dx;
	protected double dy;

	/** 충돌 캐시용 바운딩 박스 */
	private final Rectangle me   = new Rectangle();
	private final Rectangle him  = new Rectangle();

	/**
	 * @param ref 로딩할 스프라이트 경로 (없으면 null 가능)
	 * @param x   초기 X
	 * @param y   초기 Y
	 */
	public Entity(String ref, int x, int y) {
		if (ref != null) {
			this.sprite = SpriteStore.get().getSprite(ref);
		}
		this.x = x;
		this.y = y;
	}

	/* ========== 이동 & 렌더 ========== */

	/** delta(ms) 동안의 이동 처리 */
	public void move(long delta) {
		x += (dx * delta) / 1000.0;
		y += (dy * delta) / 1000.0;
	}

	/** 그리기 (스프라이트가 있을 때만) */
	public void draw(Graphics g) {
		if (sprite != null) {
			sprite.draw(g, (int) x, (int) y);
		}
	}

	/* ========== 충돌 ========== */

	/**
	 * AABB 충돌 판정 (NPE 방지 버전)
	 * 스프라이트가 하나라도 없으면 충돌하지 않는 것으로 간주한다.
	 */
	public boolean collidesWith(Entity other) {
		if (other == null || this == other) return false;

		// ✅ 스프라이트가 하나라도 없으면 충돌 검사 스킵 (NPE 방지)
		if (this.sprite == null || other.sprite == null) {
			return false;
		}

		// 나
		me.setBounds((int) x, (int) y, getWidth(), getHeight());
		// 상대
		him.setBounds(other.getX(), other.getY(), other.getWidth(), other.getHeight());

		return me.intersects(him);
	}

	/* ========== 좌표/크기 ========== */

	public int getX() { return (int) x; }
	public int getY() { return (int) y; }

	/** 필요 시 하위/외부에서 사용할 수 있도록 세터도 제공 */
	public void setX(int x) { this.x = x; }
	public void setY(int y) { this.y = y; }
	public void setX(double x) { this.x = x; }
	public void setY(double y) { this.y = y; }

	public int getWidth() {
		if (sprite != null) return sprite.getWidth();
		return 0;
	}

	public int getHeight() {
		if (sprite != null) return sprite.getHeight();
		return 0;
	}

	/* ========== 속도 제어 ========== */

	public void setHorizontalMovement(double dx) { this.dx = dx; }
	public void setVerticalMovement(double dy) { this.dy = dy; }

	public double getHorizontalMovement() { return dx; }
	public double getVerticalMovement() { return dy; }

	/* ========== 게임 로직/충돌 이벤트 ========== */

	/** 프레임 간 논리 업데이트(예: 행 이동, 속도 변경 등) */
	public abstract void doLogic();

	/** 충돌 시 호출되는 콜백 */
	public abstract void collidedWith(Entity other);
}
