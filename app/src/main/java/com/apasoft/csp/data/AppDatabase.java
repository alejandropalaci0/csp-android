package com.apasoft.csp.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Base de datos Room de la aplicación CSP.
 *
 * <p>Implementa el patrón Singleton con doble verificación de bloqueo
 * ({@code double-checked locking}) para garantizar una única instancia
 * en toda la aplicación, incluso bajo acceso concurrente.</p>
 *
 * <p>Entidades gestionadas:
 * <ul>
 *   <li>{@link ScenarioEntity} — tabla {@code scenarios}</li>
 *   <li>{@link SolutionEntity} — tabla {@code solutions}</li>
 * </ul>
 * </p>
 *
 * <p>El esquema es la versión 1; {@code exportSchema = false} desactiva
 * la exportación del JSON de esquema durante la compilación.</p>
 */
@Database(
        entities = {ScenarioEntity.class, SolutionEntity.class},
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    /** Instancia única de la base de datos; {@code volatile} garantiza visibilidad entre hilos. */
    private static volatile AppDatabase INSTANCE;

    /**
     * Proporciona acceso al DAO de escenarios y soluciones.
     *
     * @return instancia de {@link ScenarioDao}
     */
    public abstract ScenarioDao scenarioDao();

    /**
     * Devuelve la instancia única de la base de datos, creándola si es la primera llamada.
     *
     * <p>El bloque {@code synchronized} asegura que solo un hilo construye la instancia
     * cuando varios acceden concurrentemente por primera vez.</p>
     *
     * @param context contexto Android; se usa {@code getApplicationContext()} internamente
     *                para evitar fugas de memoria con contextos de Activity
     * @return instancia singleton de {@link AppDatabase}
     */
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "cutting_stock.db"
                            )
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}