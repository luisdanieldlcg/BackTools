package com.daniking.backtools.config;

import com.daniking.backtools.BackTools;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.Strictness;
import com.google.gson.TypeAdapter;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.ComponentType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("ClassExplicitlyExtendsObject") // we need to explicitly extend Object to inherit doc for equals (IntelliJ IDEA 2024.3.5 (Community Edition))
@Environment(EnvType.CLIENT)
public class ToolTransformation extends Object {
    private final static ToolTransformation EMPTY = new ToolTransformationBuilder().build();

    private final @Nullable ComponentChanges componentChanges;
    private final transient @Nullable JsonElement invalidChanges;
    private final float rotationX;
    private final float rotationY;
    private final float rotationZ;
    private final float offsetX;
    private final float offsetY;
    private final float offsetZ;
    private final @Range(from = 0, to = Integer.MAX_VALUE) float scaleX;
    private final @Range(from = 0, to = Integer.MAX_VALUE) float scaleY;
    private final @Range(from = 0, to = Integer.MAX_VALUE) float scaleZ;
    private final boolean isSymmetric;
    private final boolean isBlacklisted;

    /**
     * @see #empty()
     * @see ToolTransformationBuilder
     */
    public ToolTransformation(@Nullable ComponentChanges componentChanges,
                              float offsetX, float offsetY, float offsetZ,
                              float rotationX, float rotationY, float rotationZ,
                              @Range(from = 0, to = Integer.MAX_VALUE) float scaleX, @Range(from = 0, to = Integer.MAX_VALUE) float scaleY, @Range(from = 0, to = Integer.MAX_VALUE) float scaleZ,
                              boolean isSymmetric, boolean isBlacklisted) {
        this(componentChanges, null,
            offsetX, offsetY, offsetZ,
            rotationX, rotationY, rotationZ,
            scaleX, scaleY, scaleZ,
            isSymmetric, isBlacklisted);
    }

    protected ToolTransformation(@Nullable ComponentChanges componentChanges, @Nullable JsonElement invalidChanges,
                                 float offsetX, float offsetY, float offsetZ,
                                 float rotationX, float rotationY, float rotationZ,
                                 @Range(from = 0, to = Integer.MAX_VALUE) float scaleX, @Range(from = 0, to = Integer.MAX_VALUE) float scaleY, @Range(from = 0, to = Integer.MAX_VALUE) float scaleZ,
                                 boolean isSymmetric, boolean isBlacklisted) {
        this.componentChanges = componentChanges;
        this.invalidChanges = invalidChanges;
        this.rotationX = rotationX;
        this.rotationY = rotationY;
        this.rotationZ = rotationZ;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.scaleZ = scaleZ;
        this.isSymmetric = isSymmetric;
        this.isBlacklisted = isBlacklisted;
    }

    public static ToolTransformation empty() {
        return EMPTY;
    }

    public boolean isInvalid () {
        return invalidChanges != null;
    }

    /**
     * @return true, if the ComponentChanges hold by this Object are null / empty,
     * or if all ComponentTypes of this object's ComponentChanges map to the same optional value in the parameter.
     * <p>
     * This effectively means, in the non-trivial case, that all changes in this object must be the same as the parameter one.
     * But the parameter one may contain additional ComponentChanges, that are ignored.
     */
    public boolean matches(final @Nullable ComponentChanges otherComponentChanges) {
        if (this.isInvalid()) { // should never happen, since the ConfigHandler will sort out all invalid entries
            return false;
        } else if (this.componentChanges == otherComponentChanges) {
            return true;
        } else if (this.componentChanges == null || this.componentChanges.isEmpty()) {
            return true;
        } else if (otherComponentChanges == null || otherComponentChanges.isEmpty()) {
            return false;
        } else { // both != null
            for (final @NotNull Map.Entry<@NotNull ComponentType<?>, @NotNull Optional<?>> entry : this.componentChanges.entrySet()) {
                if (!entry.getValue().equals(otherComponentChanges.get(entry.getKey()))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * @return true, if the ComponentChanges hold by this Object are null / empty,
     * or if all ComponentTypes of this object's ComponentChanges map to the same optional value in the parameter.
     * <p>
     * This effectively means, in the non-trivial case, that all changes in this object must be the same as the parameter one.
     * But the parameter one may contain additional ComponentChanges, that are ignored.
     */
    public boolean matches(final @NotNull ToolTransformation otherToolTransformation) {
        if (otherToolTransformation.isInvalid()) {
            return false; // this.isInvalid gets checked in the other method; should never happen, since the ConfigHandler will sort out all invalid entries
        }

        return matches(otherToolTransformation.componentChanges);
    }

    public float rotationX() {
        return rotationX;
    }

    public float rotationY() {
        return rotationY;
    }

    public float rotationZ() {
        return rotationZ;
    }

    public float offsetX() {
        return offsetX;
    }

    public float offsetY() {
        return offsetY;
    }

    public float offsetZ() {
        return offsetZ;
    }

    public @Range(from = 0, to = Integer.MAX_VALUE) float scaleX() {
        return scaleX;
    }

    public @Range(from = 0, to = Integer.MAX_VALUE) float scaleY() {
        return scaleY;
    }

    public @Range(from = 0, to = Integer.MAX_VALUE) float scaleZ() {
        return scaleZ;
    }

    public boolean isSymmetric() {
        return isSymmetric;
    }

    public boolean isBlacklisted() {
        return isBlacklisted;
    }

    @Override
    public @NotNull String toString() {
        return "ToolTransformation[" +
            "componentChanges: " + componentChanges + ", " +
            "rotationX = " + rotationX + ", " +
            "rotationY = " + rotationY + ", " +
            "rotationZ = " + rotationZ + ", " +
            "offsetX = " + offsetX + ", " +
            "offsetY = " + offsetY + ", " +
            "offsetZ = " + offsetZ + ", " +
            "scaleX = " + scaleX + ", " +
            "scaleY = " + scaleY + ", " +
            "scaleZ = " + scaleZ + ", " +
            "isSymmetric = " + isSymmetric + ", " +
            "isBlacklisted = " + isBlacklisted +
            ']';
    }

    @Override
    public int hashCode() {
        return Objects.hash(componentChanges,
            rotationX, rotationY, rotationZ,
            offsetX, offsetY, offsetZ,
            scaleX, scaleY, scaleZ,
            isSymmetric, isBlacklisted);
    }

    /**
     * {@inheritDoc}
     *
     * @see #matches(ComponentChanges)
     * @see #matches(ToolTransformation)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;

        if (obj instanceof ToolTransformation other) {
            return Objects.equals(this.componentChanges, other.componentChanges) &&
                this.rotationX == other.rotationX && this.rotationY == other.rotationY && this.rotationZ == other.rotationZ &&
                this.offsetX == other.offsetX && this.offsetY == other.offsetY && this.offsetZ == other.offsetZ &&
                this.scaleX == other.scaleX && this.scaleY == other.scaleY && this.scaleZ == other.scaleZ &&
                this.isSymmetric == other.isSymmetric && this.isBlacklisted == other.isBlacklisted;
        } else {
            return false;
        }
    }

    public static class ToolTransformationBuilder {
        private @Nullable ComponentChanges changes = null;
        private float offsetX = 0f;
        private float offsetY = 0f;
        private float offsetZ = 0f;
        private float rotationX = 0f;
        private float rotationY = 0f;
        private float rotationZ = 0f;
        private @Range(from = 0, to = Integer.MAX_VALUE) float scaleX = 1f;
        private @Range(from = 0, to = Integer.MAX_VALUE) float scaleY = 1f;
        private @Range(from = 0, to = Integer.MAX_VALUE) float scaleZ = 1f;
        private boolean isSymmetric = true;
        private boolean isBlacklisted = false;
        private @Nullable JsonElement invalidComponentChanges = null;

        public @NotNull ToolTransformationBuilder componentChanges(@Nullable ComponentChanges componentChanges) {
            this.changes = componentChanges;

            return this;
        }

        protected @NotNull ToolTransformationBuilder invalidComponentChanges (@NotNull JsonElement invalidComponentChanges) {
            this.changes = null;
            this.invalidComponentChanges = invalidComponentChanges;

            return this;
        }

        public @NotNull ToolTransformationBuilder offsetX(float offsetX) {
            this.offsetX = offsetX;

            return this;
        }

        public @NotNull ToolTransformationBuilder offsetY(float offsetY) {
            this.offsetY = offsetY;

            return this;
        }

        public @NotNull ToolTransformationBuilder offsetZ(float offsetZ) {
            this.offsetZ = offsetZ;

            return this;
        }

        public @NotNull ToolTransformationBuilder rotationX(float rotationX) {
            this.rotationX = rotationX;

            return this;
        }

        public @NotNull ToolTransformationBuilder rotationY(float rotationY) {
            this.rotationY = rotationY;

            return this;
        }

        public @NotNull ToolTransformationBuilder rotationZ(float rotationZ) {
            this.rotationZ = rotationZ;

            return this;
        }

        public @NotNull ToolTransformationBuilder scaleX(float scaleX) {
            this.scaleX = Math.max(0, Math.min(scaleX, Integer.MAX_VALUE));

            return this;
        }

        public @NotNull ToolTransformationBuilder scaleY(float scaleY) {
            this.scaleY = Math.max(0, Math.min(scaleY, Integer.MAX_VALUE));

            return this;
        }

        public @NotNull ToolTransformationBuilder scaleZ(float scaleZ) {
            this.scaleZ = Math.max(0, Math.min(scaleZ, Integer.MAX_VALUE));

            return this;
        }

        public @NotNull ToolTransformationBuilder isSymmetric(boolean isSymmetric) {
            this.isSymmetric = isSymmetric;

            return this;
        }

        public @NotNull ToolTransformationBuilder isBlacklisted(boolean isNegative) {
            this.isBlacklisted = isNegative;

            return this;
        }

        public @NotNull ToolTransformation build() {
            return new ToolTransformation(
                changes, invalidComponentChanges,
                offsetX, offsetY, offsetZ, rotationX, rotationY, rotationZ,
                scaleX, scaleY, scaleZ,
                isSymmetric, isBlacklisted);
        }
    }

    public static class ToolTransformationTypAdapter extends TypeAdapter<ToolTransformation> {
        private static final @NotNull String
            COMPONENTS_KEY = "components",
            ROTATION_KEY = "rotation",
            OFFSET_KEY = "offset",
            SCALE_KEY = "scale",
            IS_SYMMETRIC_KEY = "is_symmetric",
            IS_BLACKLISTED = "is_blacklisted",
            X_KEY = "x",
            Y_KEY = "y",
            Z_KEY = "z";

        private static void warnUnknown(@NotNull JsonReader jsonReader) throws IOException {
            BackTools.LOGGER.warn("I have unexpectedly just read {} with type {} did you downgrade?", jsonReader.getPath(), jsonReader.peek());
        }

        @Override
        public void write(final @NotNull JsonWriter jsonWriter, final @NotNull ToolTransformation toolTransformation) throws IOException {
            jsonWriter.beginObject();

            if (toolTransformation.invalidChanges != null) {
                jsonWriter.name(COMPONENTS_KEY);
                Streams.write(toolTransformation.invalidChanges, jsonWriter);

            } else if (toolTransformation.componentChanges != null && !toolTransformation.componentChanges.isEmpty()) {
                jsonWriter.name(COMPONENTS_KEY);

                final Strictness strictnessBefore = jsonWriter.getStrictness();
                jsonWriter.setStrictness(Strictness.LENIENT);
                Streams.write(ComponentChanges.CODEC.encodeStart(BackTools.getConfigHandler().getDynamicJSONOps(), toolTransformation.componentChanges).getOrThrow(IOException::new), jsonWriter);
                jsonWriter.setStrictness(strictnessBefore);
            }

            if (toolTransformation.offsetX() != 0 || toolTransformation.offsetY() != 0 || toolTransformation.offsetZ() != 0) {
                jsonWriter.name(OFFSET_KEY);
                jsonWriter.beginObject();

                if (toolTransformation.offsetX() != 0) {
                    jsonWriter.name(X_KEY);
                    jsonWriter.value(toolTransformation.offsetX());
                }

                if (toolTransformation.offsetY() != 0) {
                    jsonWriter.name(Y_KEY);
                    jsonWriter.value(toolTransformation.offsetY());
                }

                if (toolTransformation.offsetZ() != 0) {
                    jsonWriter.name(Z_KEY);
                    jsonWriter.value(toolTransformation.offsetZ());
                }

                jsonWriter.endObject();
            }

            if (toolTransformation.rotationX() != 0 || toolTransformation.rotationY() != 0 || toolTransformation.rotationZ() != 0) {
                jsonWriter.name(ROTATION_KEY);
                jsonWriter.beginObject();

                if (toolTransformation.rotationX() != 0) {
                    jsonWriter.name(X_KEY);
                    jsonWriter.value(toolTransformation.rotationX());
                }

                if (toolTransformation.rotationY() != 0) {
                    jsonWriter.name(Y_KEY);
                    jsonWriter.value(toolTransformation.rotationY());
                }

                if (toolTransformation.rotationZ() != 0) {
                    jsonWriter.name(Z_KEY);
                    jsonWriter.value(toolTransformation.rotationZ());
                }

                jsonWriter.endObject();
            }

            if (toolTransformation.scaleX() != 1 || toolTransformation.scaleY() != 1 || toolTransformation.scaleZ() != 1) {
                jsonWriter.name(SCALE_KEY);
                jsonWriter.beginObject();

                if (toolTransformation.scaleX() != 1) {
                    jsonWriter.name(X_KEY);
                    jsonWriter.value(toolTransformation.scaleX());
                }

                if (toolTransformation.scaleY() != 1) {
                    jsonWriter.name(Y_KEY);
                    jsonWriter.value(toolTransformation.scaleY());
                }

                if (toolTransformation.scaleZ() != 1) {
                    jsonWriter.name(Z_KEY);
                    jsonWriter.value(toolTransformation.scaleZ());
                }

                jsonWriter.endObject();
            }

            if (!toolTransformation.isSymmetric()) {
                jsonWriter.name(IS_SYMMETRIC_KEY);
                jsonWriter.value(false);
            }

            if (toolTransformation.isBlacklisted()) {
                jsonWriter.name(IS_BLACKLISTED);
                jsonWriter.value(true);
            }

            jsonWriter.endObject();
        }

        @Override
        public @NotNull ToolTransformation read(final @NotNull JsonReader jsonReader) throws IOException {
            final ToolTransformationBuilder builder = new ToolTransformationBuilder();

            jsonReader.beginObject();
            while (jsonReader.hasNext()) {
                switch (jsonReader.nextName()) {
                    case COMPONENTS_KEY -> {
                        // no begin / end object here, the parser will take care of this
                        final JsonElement element = JsonParser.parseReader(jsonReader);

                        try {
                            builder.componentChanges(
                                ComponentChanges.CODEC.decode(BackTools.getConfigHandler().getDynamicJSONOps(), element).
                                    getOrThrow(IOException::new).getFirst()
                            );
                        } catch (IOException e) {
                            BackTools.LOGGER.warn("Skipped configured element, because it's components are invalid in current context. This may happen if a data pack is missing or it was misconfigured. {}", e.getMessage());

                            builder.invalidComponentChanges(element);
                        }
                    }
                    case OFFSET_KEY -> {
                        jsonReader.beginObject();

                        while (jsonReader.hasNext()) {
                            final @NotNull String currentName = jsonReader.nextName();
                            switch (currentName) {
                                case X_KEY -> builder.offsetX((float) jsonReader.nextDouble());
                                case Y_KEY -> builder.offsetY((float) jsonReader.nextDouble());
                                case Z_KEY -> builder.offsetZ((float) jsonReader.nextDouble());
                                default -> warnUnknown(jsonReader);
                            }
                        }

                        jsonReader.endObject();
                    }
                    case ROTATION_KEY -> {
                        jsonReader.beginObject();

                        while (jsonReader.hasNext()) {
                            final @NotNull String currentName = jsonReader.nextName();
                            switch (currentName) {
                                case X_KEY -> builder.rotationX((float) jsonReader.nextDouble());
                                case Y_KEY -> builder.rotationY((float) jsonReader.nextDouble());
                                case Z_KEY -> builder.rotationZ((float) jsonReader.nextDouble());
                                default -> warnUnknown(jsonReader);
                            }
                        }

                        jsonReader.endObject();
                    }
                    case SCALE_KEY -> {
                        jsonReader.beginObject();

                        while (jsonReader.hasNext()) {
                            final @NotNull String currentName = jsonReader.nextName();
                            switch (currentName) {
                                case X_KEY -> builder.scaleX((float) jsonReader.nextDouble());
                                case Y_KEY -> builder.scaleY((float) jsonReader.nextDouble());
                                case Z_KEY -> builder.scaleZ((float) jsonReader.nextDouble());
                                default -> warnUnknown(jsonReader);
                            }
                        }

                        jsonReader.endObject();
                    }
                    case IS_SYMMETRIC_KEY -> builder.isSymmetric(jsonReader.nextBoolean());
                    case IS_BLACKLISTED -> builder.isBlacklisted(jsonReader.nextBoolean());
                    default -> warnUnknown(jsonReader);
                }
            }
            jsonReader.endObject();

            return builder.build();
        }
    }
}
