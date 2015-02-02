package me.geakstr.voxel.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Chunk extends Mesh {
    public int[][][] cubes; // [x][y][z]

    public boolean changed;

    public int x_chunk_pos, y_chunk_pos;
    public int x_offset, y_offset;

    public Chunk(int x_chunk_pos, int y_chunk_pos) {
        super();

        this.x_chunk_pos = x_chunk_pos;
        this.y_chunk_pos = y_chunk_pos;

        this.x_offset = x_chunk_pos * World.chunk_width;
        this.y_offset = y_chunk_pos * World.chunk_length;

        this.vertices_size = World.chunk_volume * CubeManager.cube_vertices_size;
        //this.indices_size = volume * 36 + width;

        this.vertices = new float[vertices_size];
        //this.indices = new int[indices_size];

        this.cubes = new int[World.chunk_width][World.chunk_length][World.chunk_height];
        this.changed = true;
    }

    public void update() {
        changed = false;

        int next_color = 512;
        int vertices_offset = 0/*, indices_offset = 0*/;
        for (int z = 0; z < World.chunk_height; z++) {
            int[][] mark = new int[World.chunk_length][World.chunk_width];
            int[] proj = new int[mark[0].length];
            Arrays.fill(proj, -1);
            int len = 0;
            Map<Integer, int[]> coords_map = new HashMap<>();
            for (int y = 0; y < World.chunk_length; y++) {
                boolean canDown = false;
                int projFlag = -1;
                for (int x = 0; x < World.chunk_width; x++) {
                    int val = cubes[x][y][z];
                    int type = CubeManager.unpack_type(val);

                    if (type == 0) {
                        continue;
                    }

                    boolean update_coords = false;

                    if (proj[x] != -1) {
                        mark[y][x] = mark[y - 1][x];
                        projFlag = x;
                        len = 0;
                        update_coords = true;
                    } else if (((x > 0) && (CubeManager.unpack_type(cubes[x - 1][y][z]) == type) && (projFlag != x - 1))) {
                        mark[y][x] = mark[y][x - 1];
                        update_coords = true;
                    } else {
                        mark[y][x] = ++next_color;
                        coords_map.put(next_color, new int[]{x, y, x, y});
                        len = 0;
                    }

                    if (update_coords) {
                        int[] tmp = null;
                        int face = proj[x] != -1 ? proj[x] : mark[y][x - 1];
                        tmp = coords_map.get(face);
                        tmp[2] = x;
                        tmp[3] = y;
                        coords_map.put(face, tmp);
                    }

                    if (x > 0 && mark[y][x - 1] == mark[y][x]) {
                        len++;
                    }

                    canDown = !(len > 0 && !canDown) && (y < (World.chunk_length - 1) && (CubeManager.unpack_type(cubes[x][y + 1][z]) == type));

                    if (canDown) {
                        proj[x] = mark[y][x];
                    } else {
                        int tmp = len;
                        while (tmp >= 0) {
                            proj[x - tmp] = -1;
                            tmp--;
                        }
                    }
                }
            }

            for (Map.Entry<Integer, int[]> e : coords_map.entrySet()) {
                int[] coords = e.getValue();
                int x0 = coords[0], y0 = coords[1];
                int x1 = coords[2], y1 = coords[3];

                boolean[] renderable_sides = renderable_sides_2(x0, y0, x1, y1, z);
                for (int side_idx = 0; side_idx < 6; side_idx++) {
                    if (renderable_sides[side_idx]) {
                        float[] side = CubeManager.get_side(side_idx, x0 + x_offset, y0 + y_offset, x1 + x_offset, y1 + y_offset, z);

                        System.arraycopy(side, 0, vertices, vertices_offset, CubeManager.cube_side_vertices_size);
                        vertices_offset += CubeManager.cube_side_vertices_size;
                    }
                }
            }
        }

        vertices = Arrays.copyOfRange(vertices, 0, vertices_offset);
        vertices_size = vertices_offset;

        fill_buffers();
    }

    public boolean[] renderable_sides_2(int x0, int y0, int x1, int y1, int z) {
        boolean[] sides = new boolean[6];

        if (x0 == 0) {
            sides[0] = true;
        } else if (x0 > 0) {
            for (int y = y0; y <= y1; y++) {
                if (CubeManager.unpack_type(cubes[x0 - 1][y][z]) == 0) {
                    sides[0] = true;
                    break;
                }
            }
        }
        if (x1 == World.chunk_width - 1) {
            sides[1] = true;
        } else if (x1 < World.chunk_width - 1) {
            for (int y = y0; y <= y1; y++) {
                if (CubeManager.unpack_type(cubes[x1 + 1][y][z]) == 0) {
                    sides[1] = true;
                    break;
                }
            }
        }

        if (y0 == 0) {
            sides[3] = true;
        } else if (y0 > 0) {
            for (int x = x0; x <= x1; x++) {
                if (CubeManager.unpack_type(cubes[x][y0 - 1][z]) == 0) {
                    sides[3] = true;
                    break;
                }
            }
        }

        if (y1 == World.chunk_length - 1) {
            sides[2] = true;
        } else if (y1 < World.chunk_length - 1) {
            for (int x = x0; x <= x1; x++) {
                if (CubeManager.unpack_type(cubes[x][y1 + 1][z]) == 0) {
                    sides[2] = true;
                    break;
                }
            }
        }

        if (z == 0) {
            sides[4] = true;
        } else if (z > 0) {
            for (int x = x0; x <= x1; x++) {
                for (int y = y0; y <= y1; y++) {
                    if (CubeManager.unpack_type(cubes[x][y][z - 1]) == 0) {
                        sides[4] = true;
                        break;
                    }
                }
            }
        }

        if (z == World.chunk_height - 1) {
            sides[5] = true;
        } else if (z < World.chunk_height - 1) {
            for (int x = x0; x <= x1; x++) {
                for (int y = y0; y <= y1; y++) {
                    if (CubeManager.unpack_type(cubes[x][y][z + 1]) == 0) {
                        sides[5] = true;
                        break;
                    }
                }
            }
        }

        if (sides[0] &&
                x0 == 0 &&
                x_chunk_pos != 0) {
            sides[0] = false;
            for (int y = y0; y <= y1; y++) {
                if (CubeManager.unpack_type(World.chunks[x_chunk_pos - 1][y_chunk_pos].cubes[World.chunk_width - 1][y][z]) == 0) {
                    sides[0] = true;
                    break;
                }
            }
        }

        if (sides[1] &&
                x1 == World.chunk_width - 1 &&
                x_chunk_pos != World.world_size - 1) {
            sides[1] = false;
            for (int y = y0; y <= y1; y++) {
                if (CubeManager.unpack_type(World.chunks[x_chunk_pos + 1][y_chunk_pos].cubes[0][y][z]) == 0) {
                    sides[1] = true;
                    break;
                }
            }
        }

        if (sides[2] &&
                y1 == World.chunk_length - 1 &&
                y_chunk_pos != World.world_size - 1) {
            sides[2] = false;
            for (int x = x0; x <= x1; x++) {
                if (CubeManager.unpack_type(World.chunks[x_chunk_pos][y_chunk_pos + 1].cubes[x][0][z]) == 0) {
                    sides[2] = true;
                    break;
                }
            }
        }

        if (sides[3] &&
                y0 == 0 &&
                y_chunk_pos != 0) {
            sides[3] = false;
            for (int x = x0; x <= x1; x++) {
                if (CubeManager.unpack_type(World.chunks[x_chunk_pos][y_chunk_pos - 1].cubes[x][World.chunk_length - 1][z]) == 0) {
                    sides[3] = true;
                    break;
                }
            }
        }

        return sides;
    }

    public void render() {
        if (changed) {
            update();
        }
        super.render();
    }
}