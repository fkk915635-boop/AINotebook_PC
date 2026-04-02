/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        wood: {
          50: '#FDFCFB',
          100: '#F5F1E9',
          200: '#EAE0D5',
          300: '#C6AC8F',
          400: '#5E503F',
          500: '#22333B',
          accent: '#A68A64'
        }
      },
      fontFamily: {
        'sans': ['"PingFang SC"', '"Microsoft YaHei"', 'sans-serif'],
      }
    },
  },
  plugins: [],
}
