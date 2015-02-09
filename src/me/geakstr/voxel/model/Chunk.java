package me.geakstr.voxel.model;

import me.geakstr.voxel.game.Game;
import me.geakstr.voxel.math.Vector2f;
import me.geakstr.voxel.workers.ChunkWorker;

import java.util.*;

public class Chunk extends Mesh {
    public int[][][] cubes; // [x][y][z]

    public boolean changed, updating, updated;
    public boolean drawable;

    public int x_chunk_pos, y_chunk_pos, z_chunk_pos;
    public int x_offset, y_offset, z_offset;

    public Chunk(int x_chunk_pos, int y_chunk_pos, int z_chunk_pos) {
        super();

        this.x_chunk_pos = x_chunk_pos;
        this.y_chunk_pos = y_chunk_pos;
        this.z_chunk_pos = z_chunk_pos;

        this.x_offset = x_chunk_pos * World.chunk_width;
        this.y_offset = y_chunk_pos * World.chunk_length;
        this.z_offset = z_chunk_pos * World.chunk_height;

        this.cubes = new int[World.chunk_width][World.chunk_length][World.chunk_height];

        this.changed = true;
        this.updating = false;
        this.updated = true;

        this.drawable = false;
    }

    public void update() {
        if (changed && !updating && updated) {
            updated = false;
            Game.chunks_workers_executor_service.add_worker(new ChunkWorker(this));
        }

        if (updated && updating && drawable) {
            updating = false;
            prepare_render();
        }

        changed = false;
    }

    public void rebuild() {
        this.updating = true;

        Random rnd = new Random();

        List<Integer> vertices = new ArrayList<>();
        List<Integer> textures = new ArrayList<>();
        List<Float> textures_offsets = new ArrayList<>();
        List<Float> colors = new ArrayList<>();

        int next_color = 512;
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
                    if (Cube.unpack_type(this.cubes[x][y][z]) == 0) {
                        continue;
                    }

                    int type = 1;
                    boolean update_coords = false;

                    if (proj[x] != -1) {
                        mark[y][x] = mark[y - 1][x];
                        projFlag = x;
                        len = 0;
                        update_coords = true;
                    } else if (((x > 0) && (Cube.unpack_type(this.cubes[x - 1][y][z]) == type) && (projFlag != x - 1))) {
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

                    canDown = !(len > 0 && !canDown) && (y < (World.chunk_length - 1) && (Cube.unpack_type(this.cubes[x][y + 1][z]) == type));

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

                boolean[] renderable_sides = this.renderable_sides(x0, y0, x1, y1, z);

                Vector2f tex = rnd.nextBoolean() ? TextureAtlas.get_coord("cobblestone") : TextureAtlas.get_coord("dirt");
                for (int side_idx = 0; side_idx < 6; side_idx++) {
                    if (renderable_sides[side_idx]) {
                        vertices.addAll(Arrays.asList(Cube.get_side(
                                side_idx,
                                x0 + this.x_offset,
                                y0 + this.y_offset,
                                x1 + this.x_offset,
                                y1 + this.y_offset,
                                z + this.z_offset)));

                        textures.addAll(Arrays.asList(Cube.get_texture(side_idx, x0, y0, x1, y1)));
                        textures_offsets.addAll(Arrays.asList(tex.x, tex.y, tex.x, tex.y, tex.x, tex.y, tex.x, tex.y, tex.x, tex.y, tex.x, tex.y));

                        float r = 1.0f, g = 1.0f, b = 1.0f;
                        if (side_idx >= 0 && side_idx <= 3) {
                            r = 0.7f;
                            g = 0.7f;
                            b = 0.7f;
                        }
                        colors.addAll(Arrays.asList(r, g, b, r, g, b, r, g, b, r, g, b, r, g, b, r, g, b));
                    }
                }
            }
        }

        this.vertices = vertices.toArray(new Integer[vertices.size()]);
        this.textures = textures.toArray(new Integer[textures.size()]);
        this.textures_offsets = textures_offsets.toArray(new Float[textures_offsets.size()]);
        this.colors = colors.toArray(new Float[colors.size()]);

        this.vertices_size = this.vertices.length;
        this.textures_size = this.textures.length;
        this.textures_offsets_size = this.textures_offsets.length;
        this.colors_size = this.colors.length;

        this.updated = true;

        this.drawable = this.vertices_size > 0;
    }

    public boolean[] renderable_sides(int x0, int y0, int x1, int y1, int z) {
        boolean[] sides = new boolean[6];

        if (x0 > 0) {
            for (int y = y0; y <= y1; y++) {
                if (Cube.unpack_type(cubes[x0 - 1][y][z]) == 0) {
                    sides[0] = true;
                    break;
                }
            }
        }
        if (x1 < World.chunk_width - 1) {
            for (int y = y0; y <= y1; y++) {
                if (Cube.unpack_type(cubes[x1 + 1][y][z]) == 0) {
                    sides[1] = true;
                    break;
                }
            }
        }

        if (y0 > 0) {
            for (int x = x0; x <= x1; x++) {
                if (Cube.unpack_type(cubes[x][y0 - 1][z]) == 0) {
                    sides[3] = true;
                    break;
                }
            }
        }

        if (y1 < World.chunk_length - 1) {
            for (int x = x0; x <= x1; x++) {
                if (Cube.unpack_type(cubes[x][y1 + 1][z]) == 0) {
                    sides[2] = true;
                    break;
                }
            }
        }

        if (z > 0) {
            for (int x = x0; x <= x1; x++) {
                for (int y = y0; y <= y1; y++) {
                    if (Cube.unpack_type(cubes[x][y][z - 1]) == 0) {
                        sides[4] = true;
                        break;
                    }
                }
            }
        }

        if (z < World.chunk_height - 1) {
            for (int x = x0; x <= x1; x++) {
                for (int y = y0; y <= y1; y++) {
                    if (Cube.unpack_type(cubes[x][y][z + 1]) == 0) {
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
                if (Cube.unpack_type(World.chunks[z_chunk_pos][x_chunk_pos - 1][y_chunk_pos].cubes[World.chunk_width - 1][y][z]) == 0) {
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
                if (Cube.unpack_type(World.chunks[z_chunk_pos][x_chunk_pos + 1][y_chunk_pos].cubes[0][y][z]) == 0) {
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
                if (Cube.unpack_type(World.chunks[z_chunk_pos][x_chunk_pos][y_chunk_pos + 1].cubes[x][0][z]) == 0) {
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
                if (Cube.unpack_type(World.chunks[z_chunk_pos][x_chunk_pos][y_chunk_pos - 1].cubes[x][World.chunk_length - 1][z]) == 0) {
                    sides[3] = true;
                    break;
                }
            }
        }

        if (sides[4] &&
                z == 0 &&
                z_chunk_pos != 0) {
            sides[4] = false;
            for (int x = x0; x <= x1; x++) {
                for (int y = y0; y <= y1; y++) {
                    if (Cube.unpack_type(World.chunks[z_chunk_pos - 1][x_chunk_pos][y_chunk_pos].cubes[x][y][World.chunk_height - 1]) == 0) {
                        sides[4] = true;
                        break;
                    }
                }
            }
        }

        if (sides[5] &&
                z == World.chunk_height - 1 &&
                z_chunk_pos != World.world_height - 1) {
            sides[5] = false;
            for (int x = x0; x <= x1; x++) {
                for (int y = y0; y <= y1; y++) {
                    if (Cube.unpack_type(World.chunks[z_chunk_pos + 1][x_chunk_pos][y_chunk_pos].cubes[x][y][0]) == 0) {
                        sides[5] = true;
                        break;
                    }
                }
            }

        }

//        sides[0] = true;
//        sides[1] = true;
//        sides[2] = true;
//        sides[3] = true;
//        sides[4] = true;
//        sides[5] = true;

        return sides;
    }

    public void render() {
        if (changed || updating) {
            update();
        } else if (drawable) {
            super.render();
            World.chunks_in_frame++;
        }
    }
}
