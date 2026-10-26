// Recuperado de alg0.dev

int[] countingSort(int[] array) {
    int maxValue = array[0];
    for (int v : array) if (v > maxValue) maxValue = v;
    int[] count = new int[maxValue + 1];
    int[] output = new int[array.length];
    // Count occurrences
    for (int i = 0; i < array.length; i++) {
        count[array[i]]++;
    }
    // Cumulative count
    for (int i = 1; i <= maxValue; i++) {
        count[i] += count[i - 1];
    }
    // Build output (reverse for stability)
    for (int i = array.length - 1; i >= 0; i--) {
        output[count[array[i]] - 1] = array[i];
        count[array[i]]--;
    }
    return output;
}
