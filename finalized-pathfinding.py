from collections import deque
from enum import Enum
from typing import Set, List, Tuple
import time

# From previous code:
# We assume NanoBot and BLE classes are from the ArduinoPython code
from nanonav import BLE, NanoBot

# ---- Enums and classes for Wumpus World logic ----
class ActionType(Enum):
    TURN_LEFT = 1
    TURN_RIGHT = 2
    FORWARD = 3
    SHOOT = 4

class Action:
    def __init__(self, action_type: ActionType):
        self.type = action_type
    def __repr__(self):
        return f"Action({self.type})"

class Signal(Enum):
    NOTHING = 0
    BREEZE = 1
    STENCH = 2
    GLITTER = 4
    GOLD = 8

class SpaceType(Enum):
    EMPTY = 0
    HOME = 1
    PIT = 2
    WUMPUS = 3
    GOLD = 4

    def danger(self):
        return self in (SpaceType.PIT, SpaceType.WUMPUS)

class Direction(Enum):
    NORTH = (0, -1)
    EAST = (1, 0)
    SOUTH = (0, 1)
    WEST = (-1, 0)

    def rotate_left(self):
        if self == Direction.NORTH:
            return Direction.WEST
        elif self == Direction.WEST:
            return Direction.SOUTH
        elif self == Direction.SOUTH:
            return Direction.EAST
        elif self == Direction.EAST:
            return Direction.NORTH

    def rotate_right(self):
        if self == Direction.NORTH:
            return Direction.EAST
        elif self == Direction.EAST:
            return Direction.SOUTH
        elif self == Direction.SOUTH:
            return Direction.WEST
        elif self == Direction.WEST:
            return Direction.NORTH

class Simulation:
    def __init__(self, size=4):
        self.size = size
        # We'll assume start at bottom-left (0, size-1) facing NORTH
        self.start_loc = (0, self.size - 1)
        self.start_facing = Direction.NORTH

    def get_valid_adjacent(self, pos: Tuple[int, int]) -> Set[Tuple[int, int]]:
        x, y = pos
        candidates = [
            (x, y-1),  # NORTH
            (x+1, y),  # EAST
            (x, y+1),  # SOUTH
            (x-1, y)   # WEST
        ]
        return {(nx, ny) for (nx, ny) in candidates if 0 <= nx < self.size and 0 <= ny < self.size}


class Agent:
    def __init__(self, sim: Simulation):
        self.sim = sim
        self.size = sim.size
        self.possible = [
            [set([SpaceType.EMPTY, SpaceType.PIT, SpaceType.WUMPUS, SpaceType.GOLD]) for _ in range(self.size)]
            for _ in range(self.size)
        ]

        self.path = deque()
        self.visited = set()
        self.target_steps = deque()

        self.location = sim.start_loc
        self.direction = sim.start_facing
        self.found_gold = False
        self.has_arrow = True

        x, y = self.location
        self.possible[y][x].clear()
        self.possible[y][x].add(SpaceType.HOME)

    def get_actions(self, signals: Set[Signal]) -> List[Action]:
        x, y = self.location

        if Signal.GOLD in signals:
            self.found_gold = True
            self.target_steps.clear()
            self.target_steps.extend(self.pathfind(self.sim.start_loc))
            next_step = self.target_steps.popleft() if self.target_steps else None
            if next_step:
                self.path.append(self.location)
                return self.get_move_action(next_step)
            else:
                return []

        # Remove gold if not found this turn
        if SpaceType.GOLD in self.possible[y][x] and Signal.GOLD not in signals:
            self.possible[y][x].discard(SpaceType.GOLD)

        # Standing cell is safe
        self.possible[y][x] = {st for st in self.possible[y][x] if not st.danger()}
        self.visited.add(self.location)

        if self.target_steps:
            next_step = self.target_steps.popleft()
            self.path.append(self.location)
            return self.get_move_action(next_step)

        has_breeze = Signal.BREEZE in signals
        has_stench = Signal.STENCH in signals
        adjacent = self.sim.get_valid_adjacent(self.location)

        if not (has_breeze or has_stench):
            # remove danger from adjacent
            for (ax, ay) in adjacent:
                self.possible[ay][ax] = {st for st in self.possible[ay][ax] if not st.danger()}
        else:
            # If breeze but no stench, remove wumpus from adjacent
            if has_breeze and not has_stench:
                for (ax, ay) in adjacent:
                    self.possible[ay][ax].discard(SpaceType.WUMPUS)
            # If stench but no breeze, remove pit from adjacent
            if has_stench and not has_breeze:
                for (ax, ay) in adjacent:
                    self.possible[ay][ax].discard(SpaceType.PIT)

        if has_stench:
            # Remove wumpus from non-adjacent cells
            for yy in range(self.size):
                for xx in range(self.size):
                    if (xx, yy) not in adjacent and (xx, yy) != self.location:
                        self.possible[yy][xx].discard(SpaceType.WUMPUS)

        # If we have arrow, try to deduce wumpus location
        if self.has_arrow:
            possible_wumpus_locs = []
            for yy in range(self.size):
                for xx in range(self.size):
                    if SpaceType.WUMPUS in self.possible[yy][xx]:
                        possible_wumpus_locs.append((xx, yy))
            if len(possible_wumpus_locs) == 1 and not self.target_steps:
                wumpus_loc = possible_wumpus_locs[0]
                if wumpus_loc in adjacent:
                    # Prepare to shoot
                    move_needed = list(self.get_move_action(wumpus_loc))
                    if move_needed and move_needed[-1].type == ActionType.FORWARD:
                        move_needed.pop()
                    move_needed.append(Action(ActionType.SHOOT))
                    return move_needed
                else:
                    # Move closer
                    self.target_steps.clear()
                    self.target_steps.extend(self.pathfind(wumpus_loc, hug_target=True))
                    next_step = self.target_steps.popleft() if self.target_steps else None
                    if next_step:
                        self.path.append(self.location)
                        return self.get_move_action(next_step)

        if Signal.GLITTER in signals:
            # Remove gold from non-adjacent
            for yy in range(self.size):
                for xx in range(self.size):
                    if (xx, yy) not in adjacent:
                        self.possible[yy][xx].discard(SpaceType.GOLD)

            adj_glitters = [(ax, ay) for (ax, ay) in adjacent if SpaceType.GOLD in self.possible[ay][ax]]
            if len(adj_glitters) == 1:
                self.path.append(self.location)
                return self.get_move_action(adj_glitters[0])
            elif len(adj_glitters) > 1:
                safe_glitters = [(gx, gy) for (gx, gy) in adj_glitters
                                 if all(not st.danger() for st in self.possible[gy][gx])]
                if safe_glitters:
                    self.path.append(self.location)
                    return self.get_move_action(safe_glitters[0])

        glitters = []
        for yy in range(self.size):
            for xx in range(self.size):
                if SpaceType.GOLD in self.possible[yy][xx]:
                    glitters.append((xx, yy))

        if glitters and len(glitters) == 1:
            gx, gy = glitters[0]
            self.possible[gy][gx] = {SpaceType.GOLD}
            to_gold = self.pathfind((gx, gy))
            if to_gold:
                self.target_steps.clear()
                self.target_steps.extend(to_gold)
                next_step = self.target_steps.popleft() if self.target_steps else None
                if next_step:
                    self.path.append(self.location)
                    return self.get_move_action(next_step)

        # Move to safe unvisited
        safe_moves = [(ax, ay) for (ax, ay) in adjacent if all(not st.danger() for st in self.possible[ay][ax])]
        move_loc = None
        for loc in safe_moves:
            if loc not in self.visited:
                move_loc = loc
                break

        if move_loc is None:
            # Backtrack
            if self.path:
                last = self.path.pop()
                return self.get_move_action(last)
            else:
                return []
        else:
            self.path.append(self.location)
            return self.get_move_action(move_loc)

    def get_move_action(self, pos):
        x, y = self.location
        tx, ty = pos
        dx = tx - x
        dy = ty - y
        needed_dir = None
        for d in Direction:
            if d.value == (dx, dy):
                needed_dir = d
                break
        if needed_dir is None:
            return []
        actions = []
        # rotate to needed_dir
        if needed_dir == self.direction:
            actions.append(Action(ActionType.FORWARD))
        else:
            # Determine shortest turn
            left_turns = 0
            dcheck = self.direction
            while dcheck != needed_dir and left_turns < 4:
                dcheck = dcheck.rotate_left()
                left_turns += 1

            right_turns = 0
            dcheck = self.direction
            while dcheck != needed_dir and right_turns < 4:
                dcheck = dcheck.rotate_right()
                right_turns += 1

            if left_turns <= right_turns:
                for _ in range(left_turns):
                    actions.append(Action(ActionType.TURN_LEFT))
            else:
                for _ in range(right_turns):
                    actions.append(Action(ActionType.TURN_RIGHT))
            actions.append(Action(ActionType.FORWARD))

        self.update_state(actions)
        return actions

    def update_state(self, actions: List[Action]):
        for action in actions:
            if action.type == ActionType.TURN_LEFT:
                self.direction = self.direction.rotate_left()
            elif action.type == ActionType.TURN_RIGHT:
                self.direction = self.direction.rotate_right()
            elif action.type == ActionType.FORWARD:
                x, y = self.location
                dx, dy = self.direction.value
                self.location = (x + dx, y + dy)
            elif action.type == ActionType.SHOOT:
                self.has_arrow = False

    def pathfind(self, to, hug_target=False) -> List[Tuple[int,int]]:
        start = self.location
        queue = deque([start])
        visited = set([start])
        parent = {}

        while queue:
            current = queue.popleft()
            if current == to or (hug_target and to in self.sim.get_valid_adjacent(current)):
                path = []
                node = current
                while node != start:
                    path.insert(0, node)
                    node = parent[node]
                return path

            adj = self.sim.get_valid_adjacent(current)
            safe_adj = [(ax, ay) for (ax, ay) in adj if all(not st.danger() for st in self.possible[ay][ax])]

            for nxt in safe_adj:
                if nxt not in visited:
                    visited.add(nxt)
                    parent[nxt] = current
                    queue.append(nxt)
        return []


# ---- Main Control Loop Integration ----
def signals_from_hex(value: int) -> Set[Signal]:
    # value: integer from BLE (e.g. 0x00,0x01,0x02, etc.)
    # bit flags: BREEZE=1, STENCH=2, GLITTER=4, GOLD=8
    result = set()
    if value & 0x01:
        result.add(Signal.BREEZE)
    if value & 0x02:
        result.add(Signal.STENCH)
    if value & 0x04:
        result.add(Signal.GLITTER)
    if value & 0x08:
        result.add(Signal.GOLD)
    return result


def perform_actions(robot: NanoBot, actions: List[Action]):
    # Map actions to robot motor commands
    # Adjust timings and duty cycles as needed
    for action in actions:
        if action.type == ActionType.TURN_LEFT:
            # turn left 90 deg
            # One wheel forward, other backward
            robot.m1_forward(30)
            robot.m2_backward(30)
            time.sleep(1) # adjust until 90 deg turn
            robot.stop()
        elif action.type == ActionType.TURN_RIGHT:
            # turn right 90 deg
            robot.m1_backward(30)
            robot.m2_forward(30)
            time.sleep(1) # adjust for 90 deg
            robot.stop()
        elif action.type == ActionType.FORWARD:
            # move forward one cell length
            robot.m1_forward(30)
            robot.m2_forward(30)
            time.sleep(1) # adjust for one cell movement
            robot.stop()
        elif action.type == ActionType.SHOOT:
            # Implementation depends on what SHOOT means physically
            # If not applicable, just ignore or print a message
            print("Shooting (no physical action implemented).")


# Initialize BLE and Robot
ble = BLE(name="Data")
robot = NanoBot()

sim = Simulation()
agent = Agent(sim)

# Main loop:
# Start by waiting for signals from the user
while True:
    print("Waiting for signals from user (hex input)...")
    signal_val = ble.read()
    # If still the same or no change, keep waiting
    # This loop ensures we only proceed when we get a new signal from user
    start_val = signal_val
    while signal_val == start_val:
        time.sleep(0.5)
        signal_val = ble.read()

    # Now we have a new signal
    signals = signals_from_hex(signal_val)
    print("Received signals:", signals)

    actions = agent.get_actions(signals)
    print("Actions decided:", actions)

    if not actions:
        print("No actions to perform. Possibly done or stuck.")
        # may want to break or continue depending on logic
        # break
    else:
        perform_actions(robot, actions)

    # After performing actions, loop back and wait for new signals again
