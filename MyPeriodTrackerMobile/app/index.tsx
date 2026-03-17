import { useFocusEffect, useRouter } from 'expo-router';
import { Brain, Calendar, Droplets, Plus } from 'lucide-react-native';
import { useCallback, useState } from 'react';
import { ActivityIndicator, Alert, ScrollView, Text, TouchableOpacity, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { API_BASE_URL, USER_ID } from '../config/api';
import { styles } from '../styles/index-styles';
import { CycleHistoryResponse, CycleStats, Prediction } from '../types/cycles';
import { computeCycleDay, confidenceColor, daysUntil } from '../utils/cycle-helpers';


// ─── Component ─────────────────────────────────────────────────────────────────
export default function HomeScreen() {
  const router = useRouter();
  
  const [cycleDay, setCycleDay] = useState<number | null>(null);
  const [prediction, setPrediction] = useState<Prediction | null>(null);
  const [stats, setStats] = useState<CycleStats | null>(null);  const [totalCycles, setTotalCycles] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useFocusEffect(
    useCallback(() => {
      loadData();
    }, [])
  );

  const loadData = async () => {
    Alert.alert('URL', API_BASE_URL); // temporary debug
    setLoading(true);
    setError(null);
    try {
      const [cycleRes, predictionRes] = await Promise.all([
        fetch(`${API_BASE_URL}/api/agent/cycles?userId=${USER_ID}`, {
        }),
        fetch(`${API_BASE_URL}/api/agent/prediction?userId=${USER_ID}`).catch(() => null),
      ]);

      const cyclesData: CycleHistoryResponse = await cycleRes.json();

      setTotalCycles(cyclesData.totalCycles);
      setStats(cyclesData.stats ?? null);

      if(cyclesData.cycles.length > 0) {
        setCycleDay(computeCycleDay(cyclesData.cycles[0].startDate));
      }
      if(predictionRes?.ok) {
        const predictionData: Prediction = await predictionRes.json();
        setPrediction(predictionData);
      }
    } catch (e) {
      Alert.alert('Error', String(e)); // temporary debug
      setError('Could not load data.');
    } finally {
      setLoading(false);
    }
  };


  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.scrollContent}>

        <View style={styles.header}>
          <Text style={styles.greeting}>Hello!</Text>
          <Text style={styles.title}>Cycle Overview</Text>
        </View>

        {loading ? (
          <ActivityIndicator size="large" color="#FF6B6B" style={{ marginTop: 60 }} />
        ) : error ? (
          <View style={styles.errorCard}>
            <Text style={styles.errorText}>{error}</Text>
            <TouchableOpacity onPress={loadData} style={styles.retryButton}>
              <Text style={styles.retryText}>Retry</Text>
            </TouchableOpacity>
          </View>
        ) : (
          <>
            {/* Cycle Day Circle */}
            <View style={styles.cycleCircle}>
              <Droplets size={32} color="#FF6B6B" />
              <Text style={styles.dayNumber}>{cycleDay ?? '–'}</Text>
              <Text style={styles.dayLabel}>
                {cycleDay ? 'DAY OF CYCLE' : 'NO DATA YET'}
              </Text>
            </View>

            {/* AI Insight Card */}
            <View style={styles.aiCard}>
              <View style={styles.row}>
                <Brain size={20} color="#6366F1" />
                <Text style={styles.aiTitle}>AI AGENT INSIGHT</Text>
                {prediction && (
                  <View style={[styles.confidenceBadge,
                    { backgroundColor: confidenceColor(prediction.confidence) }]}>
                    <Text style={styles.confidenceText}>{prediction.confidence}</Text>
                  </View>
                )}
              </View>
              {prediction ? (
                prediction.reasons.map((reason, i) => (
                  <Text key={i} style={styles.aiText}>• {reason}</Text>
                ))
              ) : (
                <Text style={styles.aiText}>
                  {totalCycles < 2
                    ? 'Log at least 2 periods to unlock AI predictions.'
                    : 'Waiting for analysis...'}
                </Text>
              )}
            </View>

            {/* Stats Grid */}
            <View style={styles.grid}>
              <View style={styles.statBox}>
                <Calendar size={20} color="#475569" />
                <Text style={styles.statLabel}>Next Period</Text>
                <Text style={styles.statValue}>
                  {prediction ? daysUntil(prediction.predictedStartDate) : '–'}
                </Text>
              </View>
              <View style={styles.statBox}>
                <Droplets size={20} color="#475569" />
                <Text style={styles.statLabel}>Avg Cycle</Text>
                <Text style={styles.statValue}>
                  {stats ? `${stats.averageCycleLength} days` : '–'}
                </Text>
              </View>
            </View>

            <Text style={styles.subtext}>
              {totalCycles === 0
                ? 'Tap + to log your first period'
                : `${totalCycles} period${totalCycles === 1 ? '' : 's'} logged`}
            </Text>
          </>
        )}

      </ScrollView>

      <TouchableOpacity
        style={styles.fab}
        onPress={() => router.push('/log-period')}
      >
        <Plus color="white" size={30} />
      </TouchableOpacity>
    </SafeAreaView>
  );
}

