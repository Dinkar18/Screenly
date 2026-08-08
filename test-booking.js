async function run() {
  try {
    console.log("Logging in...");
    const loginRes = await fetch('http://localhost:8081/api/v1/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        email: 'puja825232@gmail.com',
        password: 'password'
      })
    });
    
    if (!loginRes.ok) {
      console.log("Login failed!", await loginRes.text());
      return;
    }
    
    const loginData = await loginRes.json();
    const token = loginData.token;
    console.log("Got token:", token.substring(0, 20) + "...");

    console.log("Fetching seats...");
    const showtimeId = '28a315a6-422c-4035-8f15-ae867ce916b8';
    const seatsRes = await fetch(`http://localhost:8081/api/v1/showtimes/${showtimeId}/seats`);
    const seatsData = await seatsRes.json();
    const availableSeat = seatsData.find(s => s.status === 'AVAILABLE');
    
    if (!availableSeat) {
      console.log("No available seats!");
      return;
    }
    console.log("Found available seat:", availableSeat.showtimeSeatId);

    console.log("Booking...");
    const bookRes = await fetch('http://localhost:8081/api/v1/bookings', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
        'Idempotency-Key': 'random-uuid-1234'
      },
      body: JSON.stringify({
        showtimeId: showtimeId,
        showtimeSeatIds: [availableSeat.showtimeSeatId]
      })
    });
    
    console.log("Success status:", bookRes.status);
    console.log("Success data:", await bookRes.text());
  } catch (err) {
    console.log("Error:", err.message);
  }
}
run();
