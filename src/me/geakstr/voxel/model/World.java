package me.geakstr.voxel.model;

import me.geakstr.voxel.game.Game;
import me.geakstr.voxel.render.Frustum;
import me.geakstr.voxel.util.OpenSimplexNoise;

import java.util.Random;

import static org.lwjgl.opengl.ARBOcclusionQuery.GL_QUERY_RESULT_ARB;
import static org.lwjgl.opengl.GL15.GL_QUERY_RESULT_AVAILABLE;
import static org.lwjgl.opengl.GL15.glGetQueryObjectui;

public class World {
    public static int world_size;
    public static int world_height;
    public static int world_volume; // world_size * world_size * world_height chunks

    public static int chunk_height; // z axis size
    public static int chunk_width; // x axis size
    public static int chunk_length; // y axis size
    public static int chunk_volume; // height * width * length

    public static Chunk[][][] chunks;

    public static int chunks_in_frame = 0;
    public static int faces_in_frame = 0;

    public static void init(int _world_size, int _world_height,
                            int _chunk_width, int _chunk_length, int _chunk_height) {
        world_size = _world_size;
        world_height = _world_height;
        world_volume = world_size * world_size * world_height;

        chunks = new Chunk[world_height][world_size][world_size];

        chunk_width = _chunk_width;
        chunk_length = _chunk_length;
        chunk_height = _chunk_height;
        chunk_volume = _chunk_height * _chunk_width * _chunk_length;

        for (int x = 0; x < world_size; x++) {
            for (int y = 0; y < world_size; y++) {
                for (int z = 0; z < world_height; z++) {
                    chunks[z][x][y] = new Chunk(x, y, z);
                    for (int i = 0; i < chunk_width; i++) {
                        for (int j = 0; j < chunk_length; j++) {
                            for (int k = 0; k < chunk_height; k++) {
                                chunks[z][x][y].blocks[i][j][k] = new Block();
                            }
                        }
                    }
                }
            }
        }
    }

    public static void gen() {
        Random rnd = new Random();
        OpenSimplexNoise noise = new OpenSimplexNoise(rnd.nextInt(Integer.MAX_VALUE));

        for (int global_x = 0; global_x < world_size * chunk_width; global_x++) {
            for (int global_y = 0; global_y < world_size * chunk_length; global_y++) {
                double global_z = ((noise.eval(global_x / 128.0, global_y / 128.0, 1.0) + 1)) * world_height * chunk_height / 2;

                int chunk_x = global_x / (chunk_width);
                int chunk_y = global_y / (chunk_length);

                int cube_x = global_x % chunk_width;
                int cube_y = global_y % chunk_length;

                int chunk_vert_size = (int) (Math.ceil(global_z / chunk_height));
                for (int chunk_z = 0; chunk_z < chunk_vert_size; chunk_z++) {
                    Chunk chunk = chunks[chunk_z][chunk_x][chunk_y];

                    int height = chunk_z == chunk_vert_size - 1 ? (int) (global_z % chunk_height) : chunk_height;

                    for (int cube_z = 0; cube_z < height; cube_z++) {
                        chunk.blocks[cube_x][cube_y][cube_z] = new Block(1);
                    }
                }
            }
        }
    }

    public static void occlusion_render() {
        for (int z = 0; z < world_height; z++) {
            for (int x = 0; x < world_size; x++) {
                for (int y = 0; y < world_size; y++) {
                    if (Frustum.chunkInFrustum(x, y, z)) {
                        chunks[z][x][y].occlusion_render();
                    }
                }
            }
        }
    }

    public static void render() {
        chunks_in_frame = 0;
        faces_in_frame = 0;

        for (int z = 0; z < world_height; z++) {
            for (int x = 0; x < world_size; x++) {
                for (int y = 0; y < world_size; y++) {
                    if (Game.occlusion && !chunks[z][x][y].empty) {
                        if (chunks[z][x][y].waiting) {
                            int result = glGetQueryObjectui(chunks[z][x][y].o_query, GL_QUERY_RESULT_AVAILABLE);
                            if (result == 1) {
                                chunks[z][x][y].waiting = false;
                                chunks[z][x][y].visible = glGetQueryObjectui(chunks[z][x][y].o_query, GL_QUERY_RESULT_ARB) > 0;
                            }
                            if (chunks[z][x][y].visible) {
                                chunks_in_frame++;
                                chunks[z][x][y].terrain_render();
                            }
                        }
                    } else if (!Game.occlusion) {
                        if (Frustum.chunkInFrustum(x, y, z)) {
                            chunks_in_frame++;
                            chunks[z][x][y].terrain_render();
                        }
                    }
                }
            }
        }
    }

    public static void destroy() {
        for (int z = 0; z < world_height; z++) {
            for (int x = 0; x < world_size; x++) {
                for (int y = 0; y < world_size; y++) {
                    if (null != chunks[z][x][y]) {
                        chunks[z][x][y].destroy();
                    }
                }
            }
        }
    }
}
