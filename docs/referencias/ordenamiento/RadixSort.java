// Recuperado de alg0.dev

void radixSort(int[] array) {
    int maxValue = array[0];
    for (int v : array) if (v > maxValue) maxValue = v;
    for (int exp = 1; maxValue / exp > 0; exp *= 10) {
        countingSortByDigit(array, exp);
    }
}
void countingSortByDigit(int[] array, int exp) {
    int n = array.length;
    int[] output = new int[n];
    int[] count = new int[10];
    for (int i = 0; i < n; i++) {
        int digit = (array[i] / exp) % 10;
        count[digit]++;
    }
    for (int i = 1; i < 10; i++) {
        count[i] += count[i - 1];
    }
    for (int i = n - 1; i >= 0; i--) {
        int digit = (array[i] / exp) % 10;
        output[count[digit] - 1] = array[i];
        count[digit]--;
    }
    for (int i = 0; i < n; i++) {
        array[i] = output[i];
    }
}
