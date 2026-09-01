# Fuel Tracker - Progressive Web App (PWA)

A responsive, offline-first Progressive Web App for tracking fuel refills, estimating driving range, and calculating odometer target milestones with real vehicle efficiency analytics.

## Features

- **Live Refuel Calculations**: Instant calculation of fuel volume (L), added range (km), and target odometer reading to reach based on current odometer, amount spent (Rs), fuel price (Rs/L), and vehicle mileage (km/L).
- **Interactive Controls**: Real-time slider and quick preset chips for refueling amounts (Rs 500, 1000, 1500, 2000) and mileage rates (12.0, 15.0, 18.0, 22.0 km/L).
- **Persistent Local Storage**: Refueling logs are saved locally in the browser and automatically advance your odometer baseline.
- **Analytics Dashboard**: Tracks all-time fuel expenditure (Rs) and historical vehicle efficiency (km/L).
- **PWA & Offline Ready**: Equipped with a Service Worker (`sw.js`) and Web App Manifest (`manifest.json`) for offline access and native-like installation on iOS, Android, and Desktop.
- **Data Portability**: Full JSON export and restore capabilities.

## How to Run & Deploy

### Option 1: Local Development
Run a local static server from the `web` folder:
```bash
# Using Node.js npx serve
npx serve web

# Or using Python 3
python3 -m http.server 3000 --directory web
```

### Option 2: Deploy to GitHub Pages / Vercel / Netlify
1. Upload the files in `/web` (`index.html`, `styles.css`, `app.js`, `manifest.json`, `sw.js`) to any static hosting service.
2. Open the URL in Chrome/Safari on your mobile device and tap **"Add to Home Screen"** or **"Install"** to use it as a standalone app.
