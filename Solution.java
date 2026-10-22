import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

public class Solution {
    private static final String PATH = "source.txt";
    private static final int CONNECTOR_COUNT = 100;

    private static final class Fragment {
        final String value;
        final int from;
        final int to;
        final int addedLength;

        Fragment(String value) {
            this.value = value;
            this.from = Integer.parseInt(value.substring(0, 2));
            this.to = Integer.parseInt(value.substring(value.length() - 2));
            this.addedLength = value.length() - 2;
        }
    }

    private static final class Frame {
        final int vertex;
        final int incomingEdge;

        Frame(int vertex, int incomingEdge) {
            this.vertex = vertex;
            this.incomingEdge = incomingEdge;
        }
    }

    private final List<Fragment> fragments = new ArrayList<>();
    private final List<List<Integer>> outgoing = new ArrayList<>(CONNECTOR_COUNT);

    private Solution() {
        for (int v = 0; v < CONNECTOR_COUNT; v++) {
            outgoing.add(new ArrayList<>());
        }
    }

    public static void main(String[] args) throws IOException {
        Path file = Path.of(args.length == 0 ? PATH : args[0]);
        Solution solver = new Solution();
        solver.readFragments(file);

        if (solver.fragments.isEmpty()) {
            System.out.println("No fragments found.");
            return;
        }

        List<Integer> path = solver.eulerianTrail();
        if (path == null) {
            path = solver.longestTrailExact();
        }

        System.out.println(solver.buildChain(path));
    }

    private void readFragments(Path file) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                for (String token : trimmed.split("[,;\\s]+")) {
                    if (!token.matches("[0-9]{4,}")) {
                        throw new IllegalArgumentException(
                            "Line " + lineNo + ": invalid fragment '" + token
                            + "'. Fragments must contain at least 4 digits."
                        );
                    }
                    int id = fragments.size();
                    Fragment fragment = new Fragment(token);
                    fragments.add(fragment);
                    outgoing.get(fragment.from).add(id);
                }
            }
        }
    }

    /*Finds a trail using ALL fragments, or returns null if impossible*/
    private List<Integer> eulerianTrail() {
        int[] in = new int[CONNECTOR_COUNT];
        int[] out = new int[CONNECTOR_COUNT];
        for (Fragment f : fragments) {
            out[f.from]++;
            in[f.to]++;
        }

        int start = fragments.get(0).from;
        int startCount = 0;
        int endCount = 0;
        
        for (int v = 0; v < CONNECTOR_COUNT; v++) {
            int balance = out[v] - in[v];
            if (balance == 1) {
                startCount++;
                start = v;
            } else if (balance == -1) {
                endCount++;
            } else if (balance != 0) {
                return null;
            }
        }
        if (!((startCount == 1 && endCount == 1)
                || (startCount == 0 && endCount == 0))) {
            return null;
        }

        int[] position = new int[CONNECTOR_COUNT];
        Deque<Frame> stack = new ArrayDeque<>();
        List<Integer> reverseTrail = new ArrayList<>(fragments.size());
        stack.push(new Frame(start, -1));

        while (!stack.isEmpty()) {
            Frame top = stack.peek();
            List<Integer> choices = outgoing.get(top.vertex);
            if (position[top.vertex] < choices.size()) {
                int edgeId = choices.get(position[top.vertex]++);
                stack.push(new Frame(fragments.get(edgeId).to, edgeId));
            } else {
                Frame done = stack.pop();
                if (done.incomingEdge != -1) {
                    reverseTrail.add(done.incomingEdge);
                }
            }
        }

        if (reverseTrail.size() != fragments.size()) {
            return null;
        }
        
        Collections.reverse(reverseTrail);
        return reverseTrail;
    }

    private List<Integer> longestTrailExact() {
        int n = fragments.size();
        boolean[] used = new boolean[n];
        int[] trail = new int[n];
        int[] cursor = new int[n];
        long currentLength;
        long bestLength = -1;
        int[] bestTrail = new int[0];

        for (int startEdge = 0; startEdge < n; startEdge++) {
            int depth = 0;
            trail[0] = startEdge;
            cursor[0] = 0;
            used[startEdge] = true;
            currentLength = fragments.get(startEdge).value.length();

            while (depth >= 0) {
                if (currentLength > bestLength) {
                    bestLength = currentLength;
                    bestTrail = Arrays.copyOf(trail, depth + 1);
                }

                int endVertex = fragments.get(trail[depth]).to;
                List<Integer> choices = outgoing.get(endVertex);

                while (cursor[depth] < choices.size()
                        && used[choices.get(cursor[depth])]) {
                    cursor[depth]++;
                }

                if (cursor[depth] == choices.size()) {
                    int edgeId = trail[depth];
                    used[edgeId] = false;
                    currentLength -= (depth == 0)
                            ? fragments.get(edgeId).value.length()
                            : fragments.get(edgeId).addedLength;
                    depth--;
                } else {
                    int nextEdge = choices.get(cursor[depth]++);
                    used[nextEdge] = true;
                    depth++;
                    trail[depth] = nextEdge;
                    cursor[depth] = 0;
                    currentLength += fragments.get(nextEdge).addedLength;
                }
            }
        }

        List<Integer> result = new ArrayList<>(bestTrail.length);
        for (int id : bestTrail) result.add(id);
        return result;
    }

    private String buildChain(List<Integer> path) {
        StringBuilder joined = new StringBuilder();
        for (int id : path) {
            String part = fragments.get(id).value;
            if (joined.length() == 0) joined.append(part);
            else joined.append(part.substring(2));
        }
        return joined.toString();
    }
}

