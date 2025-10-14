package org.newdawn.spaceinvaders.entity;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.Sprite;
import org.newdawn.spaceinvaders.SpriteStore;

/**
 * An entity which represents one of our space invader aliens.
 *
 * 원본 흐름 유지:
 * - 좌우로 움직이며 화면 경계에 닿으면 game.updateLogic() 요청
 * - doLogic()에서 방향 반전 + 아래로 하강
 * - 충돌 처리는 원본과 같이: 여기서는 별도 처리 없음(ShotEntity가 처리)
 *   -> 만약 30% 폭탄 드랍(좌표 필요)을 Alien에서 직접 처리하길 원하면
 *      ShotEntity 쪽 로직을 비활성화하고 여기에서 처리하도록 별도 버전 줄 수 있음.
 */
public class AlienEntity extends Entity {
	/** horizontal speed */
	private double moveSpeed = 75;
	/** game ref */
	private Game game;
	/** animation frames */
	private Sprite[] frames = new Sprite[4];
	/** frame change timer */
	private long lastFrameChange;
	/** frame duration (ms) */
	private long frameDuration = 250;
	/** frame index */
	private int frameNumber;

	/**
	 * Create a new alien entity
	 *
	 * @param game The game in which this entity is being created
	 * @param x The initial x location of this alien
	 * @param y The initial y location of this alien
	 */
	public AlienEntity(Game game,int x,int y) {
		super("sprites/alien.gif",x,y);

		// setup animation frames
		frames[0] = sprite;
		frames[1] = SpriteStore.get().getSprite("sprites/alien2.gif");
		frames[2] = sprite;
		frames[3] = SpriteStore.get().getSprite("sprites/alien3.gif");

		this.game = game;
		dx = -moveSpeed; // start moving left
	}

	/** Move based on time elapsed (and drive animation) */
	public void move(long delta) {
		// animation timing
		lastFrameChange += delta;
		if (lastFrameChange > frameDuration) {
			lastFrameChange = 0;
			frameNumber++;
			if (frameNumber >= frames.length) frameNumber = 0;
			sprite = frames[frameNumber];
		}

		// edge -> request logic update (row drop + reverse)
		if ((dx < 0) && (x < 10)) {
			game.updateLogic();
		}
		if ((dx > 0) && (x > 750)) {
			game.updateLogic();
		}

		super.move(delta);
	}

	/** Update the game logic related to aliens */
	public void doLogic() {
		// reverse horizontal movement and move down a bit
		dx = -dx;
		y += 10;

		// bottom reached -> player dies (원본 유지, 필요시 조정)
		if (y > 570) {
			game.notifyDeath();
		}
	}

	/** Collisions with aliens are handled elsewhere (e.g., ShotEntity) */
	public void collidedWith(Entity other) {
		// no-op (원본과 동일)
	}
}
