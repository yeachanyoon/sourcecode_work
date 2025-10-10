package org.newdawn.spaceinvaders;

import java.io.Serializable;

public class PlayerScore implements Serializable, Comparable<PlayerScore> {
    private static final long serialVersionUID = 1L; // 직렬화를 위한 ID
    private final String name;
    private final int score;

    public PlayerScore(String name, int score) {
        this.name = name;
        this.score = score;
    }

    public String getName() {
        return name;
    }

    public int getScore() {
        return score;
    }

    @Override
    public int compareTo(PlayerScore other) {
        // 점수가 높은 순서대로 정렬하기 위해 내림차순으로 비교
        return Integer.compare(other.score, this.score);
    }
}