import defaultTheme from 'tailwindcss/defaultTheme'

/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      // Paleta semántica respaldada por variables CSS (ver src/index.css).
      colors: {
        // Superficies / encuadre
        bg: 'var(--color-bg)',
        surface: 'var(--color-surface)',
        border: 'var(--color-border)',
        // Marca (jerarquía) — distinta de primary (acción)
        brand: 'var(--color-brand)',
        // Acción
        primary: {
          DEFAULT: 'var(--color-primary)',
          hover: 'var(--color-primary-hover)',
          active: 'var(--color-primary-active)',
          50: 'var(--color-primary-50)',
          200: 'var(--color-primary-200)',
          700: 'var(--color-primary-700)',
        },
        // Estado
        danger: {
          DEFAULT: 'var(--color-danger)',
          hover: 'var(--color-danger-hover)',
          50: 'var(--color-danger-50)',
          200: 'var(--color-danger-200)',
          700: 'var(--color-danger-700)',
        },
        success: {
          DEFAULT: 'var(--color-success)',
          50: 'var(--color-success-50)',
          200: 'var(--color-success-200)',
          700: 'var(--color-success-700)',
        },
        warning: {
          DEFAULT: 'var(--color-warning)',
          50: 'var(--color-warning-50)',
          200: 'var(--color-warning-200)',
          700: 'var(--color-warning-700)',
        },
        muted: 'var(--color-muted)',
      },
      fontFamily: {
        sans: ['Inter', ...defaultTheme.fontFamily.sans],
      },
    },
  },
  plugins: [],
}
