package org.newdawn.spaceinvaders.entity;

import org.newdawn.spaceinvaders.Game;

/**
 * The entity that represents the player's ship.
 */
public class ShipEntity extends Entity {
	/** The game in which the ship exists */
	private Game game;

	/**
	 * Construct a new entity to represent the player's ship
	 *
	 * @param game The game in which the ship is being created
	 * @param ref The reference to the sprite to show for the ship
	 * @param x The initial x location of the player's ship
	 * @param y The initial y location of the player's ship
	 */
	public ShipEntity(Game game, String ref, int x, int y) {
		super(ref, x, y);
		this.game = game;
	}

	/**
	 * Request that the ship move itself based on an elapsed amount of time
	 *
	 * @param delta The time that has elapsed since last move (ms)
	 */
	@Override
	public void move(long delta) {
		// Prevent ship from leaving the window boundaries
		if ((dx < 0) && (x < 10)) {
			return;
		}
		if ((dx > 0) && (x > 750)) {
			return;
		}

		super.move(delta);
	}

	/**
	 * Notification that the player's ship has collided with something
	 *
	 * @param other The entity with which the ship has collided
	 */
	@Override
	public void collidedWith(Entity other) {
		// collisions with aliens or alien shots result in losing a heart
		if (other instanceof AlienEntity || other.getClass().getSimpleName().equals("AlienShotEntity")) {
			game.loseHeart();
		}
	}

	/**
	 * Implementation of abstract method from Entity
	 * Ship doesn't need to perform per-frame logic updates, so leave empty.
	 */
	@Override
	public void doLogic() {
		// Ship has no periodic logic to update
	}
}
