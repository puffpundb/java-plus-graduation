package ru.practicum.aggregator.service;

public class Calculator {
	public static Double calculateSimilarity(Double minSum, Double sumA, Double sumB) {
		if (sumA <= 0 || sumB <= 0) return 0.0;
		return minSum / Math.sqrt(sumA * sumB);
	}
}
