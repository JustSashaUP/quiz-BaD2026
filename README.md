# Number Fragments — My Graph Theory Notes

These are the notes I made while trying to solve the number-fragment puzzle. I started by imagining each fragment as a bridge, researched Eulerian paths, and eventually realized that the actual problem is a **longest trail in a directed graph**.

## 1. Understanding the puzzle

The input file contains an arbitrary number of fragments. Each fragment is a string of digits with a length of at least four:

```text
1234
608017
248460
177092
```

I need to assemble the **longest possible sequence of digits**, following these rules:

1. Every fragment has an **input connector** (its first two digits) and an **output connector** (its last two digits).
2. Fragment `A` can be followed by fragment `B` only if `out(A) == in(B)`.
3. Each occurrence of a fragment may be used **at most once**. However, the same connector may be visited many times.
4. Two matching connector digits overlap: they are written only once in the final number.
5. Fragments may have different lengths. The objective is the **greatest number of digits in the combined chain**, not necessarily the greatest number of fragments.
6. A fragment's direction cannot be reversed.

For example, `608017` can be understood as a bridge:

```text
fragment: 608017

 (60) ----- (17)
  IN        OUT
    middle: 80

```

The middle digits (`80`) are part of the fragment's content. They are **not extra graph vertices or edges**.

For example:

```text
248460 - 608017 - 177092
 24-60    60-17    17-92

248460 + 8017 + 7092 = 24846080177092
```

The output contains 14 digits, because the two connectors overlap at each join.

## 2. My first idea: bridges and Euler's theorem

Euler's theorem concerns the possibility of traversing **every edge exactly once**.

First, I considered an **undirected graph**, where edges have no direction. The *degree* of a vertex, `deg(v)`, is the number of edges incident to it (a self-loop counts twice).

For a connected undirected graph with at least one edge:

- **0 odd-degree vertices** → an **Eulerian circuit** exists: traverse every edge once and return to the starting vertex.
- **Exactly 2 odd-degree vertices** → an **open Eulerian trail** exists: start at one odd vertex and finish at the other.
- **More than 2 odd-degree vertices** → no trail can traverse *every* edge exactly once.

**Correction to my initial note:** I originally marked the `0 odd-degree` case as false. That was a mistake; it is **true** for a connected graph. All-even degrees are exactly what a closed Eulerian traversal needs.

My original graph example:

```text
B-----C
|\    |
| \   |
|  \  |
A-----D
```

Degrees:

```text
deg(A) = 2 (even)
deg(B) = 3 (odd)
deg(C) = 2 (even)
deg(D) = 3 (odd)
```

There are exactly two odd vertices, `B` and `D`, so an Eulerian trail exists:

```text
B -> A -> D -> B -> C -> D
```

Five edges are used exactly once. Vertices are allowed to repeat; **edges are not**.

### Why the odd/even rule works

Whenever I arrive at an *intermediate* vertex, I must leave it through another unused edge. The edges are used in pairs: **one to enter, one to leave**. The starting vertex can have one extra departure, and the ending vertex can have one extra arrival. That explains the two odd-degree exceptions.

## 3. Important discovery: my puzzle is directed

The undirected odd/even rule is **not sufficient** for this puzzle.

For a fragment such as `608017`:

```text
60 ---> 17
```

I may continue with a fragment starting in `17`, such as `177092` or `179901`. I may **not** reverse `608017` and travel from `17` back to `60` using the same fragment.

My earlier way of drawing fragments as `(60)-80-(17)` was useful visually, but it was misleading for graph theory: `80` is not a connector here. **The whole fragment is one directed edge**, regardless of its length.

Therefore I need a **directed multigraph**:

- **Vertex:** a two-digit connector, from `00` to `99` (at most 100 possible vertices).
- **Directed edge:** one input fragment, from its first two digits to its last two digits.
- **Edge identity:** its individual occurrence in the file, so duplicate fragment values would still represent separate edges.
- **Edge weight:** how many new digits the fragment contributes when attached after another fragment.

A self-loop is possible. For example, `123412` is an edge `12 -> 12`. It is allowed and may be used once.

I must also retain leading zeros: `007148` is a valid fragment, so fragments should be stored as **strings**, not numbers.

## 4. Directed Eulerian theorem: in-degree and out-degree

For each connector `v`, I count:

- `in(v)`: how many fragments **end** at connector `v`.
- `out(v)`: how many fragments **start** at connector `v`.

Why? When I enter an intermediate connector using one fragment, I need an unused fragment that starts at that connector in order to continue. Across a complete traversal, arrivals and departures must match.

For a directed **Eulerian circuit**:

```text
in(v) = out(v)    for every vertex v
```

For a directed **open Eulerian trail**:

```text
out(start) = in(start) + 1
in(end)    = out(end) + 1
in(v)      = out(v)       for every other vertex
```

Also, all vertices incident to an edge must lie in the same **weakly connected component** (connected if I temporarily ignore arrow directions). Without that condition, balanced degrees alone are not enough.

A simple example:

```text
248460: 24 -> 60
608017: 60 -> 17
177092: 17 -> 92
```

| Vertex | In | Out | Interpretation |
| --- | ---: | ---: | --- |
| `24` | 0 | 1 | Start |
| `60` | 1 | 1 | Intermediate |
| `17` | 1 | 1 | Intermediate |
| `92` | 1 | 0 | End |

So an Eulerian trail exists, and the resulting number is `24846080177092`.

**Important distinction:** These degree conditions determine whether I can use **all edges of the graph**. They do not, by themselves, solve the problem of choosing the **best subset** of edges.

## 5. Branching: why choosing the first available edge is dangerous

Originally, I sketched branches and wondered what happens if there are multiple possible continuations:

```text
          /608171---711224
608017 ----608272---722485\           122472
          \608373\         or        /
                  \_725585---\858224/
```

This was **only an exploratory sketch**. Not every drawn connection follows `out(previous) == in(next)`, so I cannot treat it as a verified chain. That was another useful lesson: I need to construct edges from their actual two-digit endpoints, rather than connect fragments just because they look related.

Here is a valid branching example:

```text
608017: 60 -> 17
179901: 17 -> 01
177092: 17 -> 92
926017: 92 -> 17
```

At `17`, there are two outgoing choices. If I immediately take `179901`, I reach `01` and stop, leaving other edges unused.

But all four edges **can** be used:

```text
60 -> 17 -> 92 -> 17 -> 01

608017 -> 177092 -> 926017 -> 179901

Result: 608017709260179901
```

This is the reason a simple greedy algorithm (always take the first matching unused fragment and never reconsider) does not always work.

## 6. Hierholzer's algorithm

**Hierholzer's algorithm** constructs an Eulerian trail or circuit **when one exists**. The key idea is to keep exploring unused directed edges, but build the answer in reverse while backtracking.

Steps:

1. Check the directed Eulerian degree conditions.
2. Pick the required starting vertex for an open trail; for a circuit, pick any vertex with an outgoing edge.
3. Push that vertex onto a stack.
4. While the stack is not empty:
   - If its top vertex has an unused outgoing edge, take that edge and push its destination.
   - Otherwise, pop the vertex and record the edge by which it was reached.
5. Reverse the recorded edge sequence.
6. Verify that **every input edge was used**. This also detects disconnected components.

Pseudocode (recording **edge IDs**, not just vertices):

```text
stack = [(startVertex, noIncomingEdge)]
reverseTrail = []

while stack is not empty:
    (v, incomingEdge) = stack.top

    if v has an unused outgoing edge e:
        mark e used
        stack.push((e.to, e.id))
    else:
        stack.pop()
        if incomingEdge exists:
            reverseTrail.append(incomingEdge)

trail = reverse(reverseTrail)
if trail.size != totalFragments:
    no full Eulerian trail exists
```

With adjacency lists and a cursor for each vertex, the algorithm takes **`O(V + E)`** time, where `V` is the number of connector vertices and `E` is the number of fragments. It is very efficient for the all-fragments case.

## 7. But my task asks for the *longest* chain

Here is the limitation I discovered: **an entire graph can fail Euler's conditions and still contain a long valid trail**.

For example, suppose these fragments are present:

```text
608017
179901
177092
926017
358844
```

The final fragment (`35 -> 44`) is in a separate component. I cannot build one trail using all five fragments, but I can still use the first four.

So the general task is the **maximum-weight directed trail problem**:

> Find a trail that follows the arrows, never repeats an edge, and maximizes the total number of digits after merging the overlapping connectors.

For a trail of `k` fragments `f1, f2, ..., fk`, its length is:

```text
length = len(f1) + sum(len(fi) - 2), for i = 2..k
       = 2 + sum(len(fi) - 2), for i = 1..k
```

Each fragment contributes `len(fragment) - 2` new digits after a join. Because the first fragment supplies the initial two digits, the overall formula adds `2`.

If **all fragments contain 6 digits**, this becomes:

```text
length = 4 * numberOfFragmentsUsed + 2
```

When fragment lengths vary, maximizing the **number of fragments** and maximizing the **number of digits** are not necessarily the same objective. I need to maximize the number of digits.

### General exact approach: DFS with backtracking

When a complete Eulerian trail does not exist, I can search through possible trails:

1. Choose each fragment in turn as a possible starting edge.
2. Keep its ending connector as the current vertex.
3. Explore only outgoing edges from that connector.
4. Skip edges already used in the current trail.
5. Add the next edge, update the chain length, and recurse (or use an explicit stack).
6. Whenever the current length exceeds the best length found so far, record the trail.
7. Backtrack: undo the last edge and try another continuation.

```text
search(currentVertex, currentLength):
    if currentLength > bestLength:
        save current trail

    for edge in outgoing[currentVertex]:
        if edge is not used:
            mark edge used
            add edge to current trail
            search(edge.to, currentLength + len(edge.value) - 2)
            remove edge from current trail
            unmark edge
```

This can find the **exact** longest result, but its worst-case runtime is **exponential** in the number of fragments. Even with only 100 possible connectors, there can be an enormous number of different edge sequences.

A practical combined strategy is therefore:

```text
Read input -> Build directed multigraph
           -> Check Eulerian conditions and connectivity
                | yes -> Hierholzer (use all fragments)
                | no  -> Exact longest-trail search
           -> Reconstruct the number from chosen edge IDs
```

For large non-Eulerian inputs, exhaustive DFS may be impractical. Possible improvements include decomposing weakly connected components, branch-and-bound, and an integer-optimization model with degree-balance and connectivity constraints. None of these turns the general longest-trail problem into an always-fast problem.

## 8. Implementation details I should remember

- Use `String` for fragment values (leading zeros matter).
- Use `Map<Connector, List<EdgeId>>` or a 100-element adjacency list for outgoing edges.
- Store each edge's `from`, `to`, original string, and unique ID.
- Use a `boolean[] used` (or equivalent) so each fragment occurrence is used once.
- Treat repeated connectors as normal: **revisiting vertices is allowed**.
- When reconstructing the answer, append the complete first fragment, then append `substring(2)` for each subsequent fragment.
- If two solutions have the same maximal length, the problem would need an extra rule to say which one to output (for example, first found or lexicographically largest). Without such a rule, either is a valid longest answer.
- Use `long` for length calculations if the accumulated length may exceed `int` range; a very long final output also requires sufficient memory.

## 9. Checking my larger input

My `source.txt` example contains **142 fragments**, all six digits long. I found this valid chain:

```text
716890565777794373556482064083064121343222972497868721751153303887119612013153750510417085100512022142771171781184759586758823141707943693917372699031562942007148831088658432465190816374922650246027977309775416923314139340173379890062028614605676733227368753511935367493
```

Verification:

- **67 fragments** are used, with no repetitions.
- Every fragment belongs to the input file.
- All consecutive connectors match.
- The final string has **270 digits**, since `4 × 67 + 2 = 270`.
- The chain begins at connector `71` and ends at connector `93`.
- An additional exact integer-optimization check, enforcing the directed degree conditions **and connectivity**, confirmed that **67 is the maximum** for this particular 142-fragment input. Other optimal chains may also exist.

This last verification matters: obtaining a long valid chain is not, by itself, proof that it is the longest.

## 10. Conclusion — what I learned

My initial intuition about **bridges** was right, but I had to refine the graph model:

1. A fragment is **one directed edge**, not a sequence of smaller graph edges.
2. Two-digit connectors are the graph's vertices.
3. The undirected Euler odd/even-degree theorem was a useful introduction, but my real graph needs **in-degree and out-degree**.
4. **Hierholzer** efficiently solves the special case where **all fragments** can be joined into one trail.
5. **Longest directed trail** is the real general problem when only a subset of fragments can be used.
6. Because fragments can have different lengths, the actual objective is **maximum resulting digit length**.

The main realization: **Euler's theorem tells me whether I can use every bridge; it does not automatically tell me which subset of bridges produces the longest chain.**

### Running my Java implementation

With `Solution.java` and `source.txt` in the same directory:

```bash
javac Solution.java
java Solution source.txt
```

The program should print the combined digit chain. For large non-Eulerian graphs, its exact backtracking phase may take a long time.
