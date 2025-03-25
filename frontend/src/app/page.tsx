"use client"

export default function Home() {
  const handleLogin = () => {
    // Redirige vers le backend qui gère la redirection vers Spotify
    window.location.href = 'http://localhost:8080/api/auth/spotify/login';
  };

  return (
    <div style={{ textAlign: 'center', marginTop: '2rem' }}>
      <h1>Connexion avec Spotify</h1>
      <button onClick={handleLogin}>
        Se connecter avec Spotify
      </button>
    </div>
  );
}
