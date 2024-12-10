# Wumpus World Project

This repository contains two main branches that allow you to reproduce and test our Wumpus World solving algorithm. Depending on your available resources, you can choose between running the algorithm on Arduino robots or simulating it in a Minecraft virtual environment.

## Code Branch Overview

### Main Branch (`main`)
This branch contains MicroPython code for running the algorithm on Arduino robots.

#### How to Set Up
1. Clone the repository:
   ```bash
   git clone https://github.com/RichardJSun/WumpusWorld.git
   ```
2. Open OpenMV and load the code from the repository.
3. Attach your robot via USB.
4. Run the code.

#### Features
- The robot moves across tiles and uses pre-programmed hex values to transcribe hints.
- Pair your machine with the [LightBlue](https://apps.apple.com/us/app/lightblue/id557428110) app on your phone or laptop to input clues.
- Adjust the movement speed or distance if your robot doesn't traverse tiles accurately.

**Note:** Robot movement may require manual tuning for optimal performance.

---

### Minecraft Branch (`minecraft`)
This branch lets you simulate the algorithm in a virtual Minecraft environment if you don't have access to a robot.

#### How to Set Up
1. Clone the repository:
   ```bash
   git clone https://github.com/RichardJSun/WumpusWorld.git
   ```
2. Open the `SIMULATION` folder in IntelliJ (preferred) or VSCode.
   - **Do not open the entire repository**.

   ![image](https://github.com/user-attachments/assets/b1680394-db0e-4eb2-8a16-94c24c0d7435)

3. Ensure your IDE recognizes the `.gradle` folder:
   - If it doesn't, manually open it and install the needed dependencies.
4. Build the project:
   ```bash
   ./gradlew build
   ```
5. Run the `Minecraft Client` configuration.

   ![image](https://github.com/user-attachments/assets/8b3bfcba-2fba-4079-b34f-8dc8009b1a8d)

**Note:** The first-time compilation may take up to **4 minutes**.

---

## Running the Minecraft Simulation

1. Launch the Minecraft Developer Edition Client.
2. Create a new single-player world:
   - **World Configuration**: Superflat
   - **Customize Layers**: Remove all layers.
   - **Game Mode**: Creative
   - **Allow Commands**: Enabled
   - **Game Rules**: 
     - Disable weather, advance time/day, all spawning, and mob loot.

   [Watch a video on how to create the Minecraft world with the right configurations.](https://share.cleanshot.com/Q85XxV15)

3. Once in the world:
   - Teleport to the starting coordinates:
     ```bash
     /tp 0 0 0
     ```
   - Build a sample board:
     ```bash
     /wumpusworld build test
     ```
   - Fix any desync issues by breaking and replacing blocks, then run the build command again.

4. Simulate the solving process:
   ```bash
   /wumpusworld simulate
   ```

---

### Adding Custom Boards

#### Option 1: Build in-game
1. Use the 4x4 grid to create a custom board with the following block types:
   - **Gold**: Gold Block
   - **Wumpus**: Redstone Block
   - **Pit**: Empty (no block)
   - **Safe Square**: Bedrock
   - **Spawn Point**: Diamond Block
2. Run the simulation:
   ```bash
   /wumpusworld simulate
   ```

#### Option 2: Save boards as `.txt` files
1. Navigate to the `/boards` directory.

![image](https://github.com/user-attachments/assets/2b8aaf61-0cfe-4530-8a2b-8b334844a835)

2. Create a new `.txt` file with a 4x4 character grid:
   - `P`: Pit
   - `G`: Gold
   - `H`: Home/Spawn Point
   - `E`: Empty/Normal Square
   - `W`: Wumpus
3. Example file:
   ```
   HEEE
   EPEW
   GGEP
   EEEE
   ```
4. Save the file and re-run (quit and run again) the program or hot reload. Then build it in-game with the command:
   ```bash
   /wumpusworld build [filename]
   ```
5. Simulate the board:
   ```bash
   /wumpusworld simulate
   ```

---

**Note:** For any issues or bugs, please create an issue in the repository.

