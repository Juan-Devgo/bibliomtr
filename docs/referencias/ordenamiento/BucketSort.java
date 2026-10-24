// Recuperado de alg0.dev

void bucketSort(int[] array, int bucketSize) {
    if (array.length == 0) return;
    // 1. Find min and max values
    int minValue = array[0];
    int maxValue = array[0];
    for (int i = 1; i < array.length; i++) {
        if (array[i] < minValue) minValue = array[i];
        else if (array[i] > maxValue) maxValue = array[i];
    }
    // 2. Initialize buckets
    int bucketCount = (maxValue - minValue) / bucketSize + 1;
    List<List<Integer>> buckets = new ArrayList<>();
    for (int i = 0; i < bucketCount; i++) {
        buckets.add(new ArrayList<>());
    }
    // 3. Distribute elements into buckets
    for (int i = 0; i < array.length; i++) {
        int bucketIndex = (array[i] - minValue) / bucketSize;
        buckets.get(bucketIndex).add(array[i]);
    }
    // 4. Sort buckets and concatenate
    int idx = 0;
    for (int i = 0; i < buckets.size(); i++) {
        Collections.sort(buckets.get(i));
        for (int j = 0; j < buckets.get(i).size(); j++) {
            array[idx++] = buckets.get(i).get(j);
        }
    }
}
