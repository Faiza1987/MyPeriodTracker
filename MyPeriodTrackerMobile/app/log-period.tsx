import DateTimePicker from '@react-native-community/datetimepicker';
import { useRouter } from 'expo-router';
import { CheckCircle, ChevronLeft, Droplets } from 'lucide-react-native';
import { useState } from 'react';
import {
    ActivityIndicator,
    Alert,
    Platform,
    ScrollView,
    Text,
    TextInput,
    TouchableOpacity,
    View
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { API_BASE_URL, USER_ID } from '../config/api';
import { styles } from '../styles/log-period-styles';
import { CervicalMucus, FlowIntensity } from '../types/log-period';




// ─── Component ─────────────────────────────────────────────────────────────────
export default function LogPeriodScreen() {
  const router = useRouter();

// ─── Form State ─────────────────────────────────────────────────────────────────
  const [periodStart, setPeriodStart] = useState(new Date());
  const [showDatePicker, setShowDatePicker] = useState(false);
  const [flowIntensity, setFlowIntensity] = useState<FlowIntensity>(null);
  const [cervicalMucus, setCervicalMucus] = useState<CervicalMucus>(null);
  const [stressLevel, setStressLevel] = useState<number | null>(null);
  const [isIll, setIsIll] = useState(false);
  const [notes, setNotes] = useState('');

// ─── UI State ─────────────────────────────────────────────────────────────────
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);

  const formatDate = (date: Date) =>
    date.toLocaleDateString('en-GB', { day: 'numeric', month: 'long', year: 'numeric' });

  const handleSubmit = async () => {
    setLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/api/agent/period`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          userId: USER_ID,
          periodStart: periodStart.toISOString().split('T')[0], // YYYY-MM-DD
          flowIntensity: flowIntensity ?? undefined,
          cervicalMucus: cervicalMucus ?? undefined,
          stressLevel: stressLevel ?? undefined,
          isIll,
          notes: notes.trim() || undefined,
        }),
      });

      if (!response.ok) throw new Error(`Server error: ${response.status}`);

      setSuccess(true);
      setTimeout(() => {
        setSuccess(false);
        router.back();
      }, 1500);
    } catch (error) {
      Alert.alert(
        'Could not save',
        'Make sure your Spring Boot backend is running and your phone is on the same Wi-Fi as your Mac.',
      );
    } finally {
      setLoading(false);
    }
  };

  if (success) {
    return (
      <SafeAreaView style={styles.successContainer}>
        <CheckCircle size={64} color="#22C55E" />
        <Text style={styles.successText}>Period logged!</Text>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.container}>

      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={styles.backButton}>
          <ChevronLeft size={24} color="#1E293B" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Log Period</Text>
        <View style={{ width: 40 }} />
      </View>

      <ScrollView contentContainerStyle={styles.scrollContent}>

        {/* Date Picker */}
        <View style={styles.section}>
          <Text style={styles.sectionLabel}>Period Start Date</Text>
          <TouchableOpacity
            style={styles.dateButton}
            onPress={() => setShowDatePicker(true)}
          >
            <Droplets size={18} color="#FF6B6B" />
            <Text style={styles.dateText}>{formatDate(periodStart)}</Text>
          </TouchableOpacity>
          {showDatePicker && (
            <DateTimePicker
              value={periodStart}
              mode="date"
              display={Platform.OS === 'android' ? 'calendar' : 'spinner'}
              maximumDate={new Date()}
              onChange={(_, selectedDate) => {
                setShowDatePicker(false);
                if (selectedDate) setPeriodStart(selectedDate);
              }}
            />
          )}
        </View>

        {/* Flow Intensity */}
        <View style={styles.section}>
          <Text style={styles.sectionLabel}>Flow Intensity</Text>
          <View style={styles.chipRow}>
            {(['LIGHT', 'MEDIUM', 'HEAVY'] as FlowIntensity[]).map((option) => (
              <TouchableOpacity
                key={option!}
                style={[styles.chip, flowIntensity === option && styles.chipSelected]}
                onPress={() => setFlowIntensity(flowIntensity === option ? null : option)}
              >
                <Text style={[styles.chipText, flowIntensity === option && styles.chipTextSelected]}>
                  {option}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>

        {/* Cervical Mucus */}
        <View style={styles.section}>
          <Text style={styles.sectionLabel}>Cervical Mucus</Text>
          <View style={styles.chipRow}>
            {(['DRY', 'STICKY', 'CREAMY', 'EGG_WHITE'] as CervicalMucus[]).map((option) => (
              <TouchableOpacity
                key={option!}
                style={[styles.chip, cervicalMucus === option && styles.chipSelected]}
                onPress={() => setCervicalMucus(cervicalMucus === option ? null : option)}
              >
                <Text style={[styles.chipText, cervicalMucus === option && styles.chipTextSelected]}>
                  {option!.replace('_', ' ')}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>

        {/* Stress Level */}
        <View style={styles.section}>
          <Text style={styles.sectionLabel}>Stress Level</Text>
          <View style={styles.chipRow}>
            {[1, 2, 3, 4, 5].map((level) => (
              <TouchableOpacity
                key={level}
                style={[styles.stressChip, stressLevel === level && styles.chipSelected]}
                onPress={() => setStressLevel(stressLevel === level ? null : level)}
              >
                <Text style={[styles.chipText, stressLevel === level && styles.chipTextSelected]}>
                  {level}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
          <Text style={styles.stressHint}>1 = calm · 5 = very stressed</Text>
        </View>

        {/* Illness Toggle */}
        <View style={styles.section}>
          <Text style={styles.sectionLabel}>Were you ill this cycle?</Text>
          <View style={styles.chipRow}>
            <TouchableOpacity
              style={[styles.chip, isIll && styles.chipSelected]}
              onPress={() => setIsIll(true)}
            >
              <Text style={[styles.chipText, isIll && styles.chipTextSelected]}>Yes</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[styles.chip, !isIll && styles.chipSelected]}
              onPress={() => setIsIll(false)}
            >
              <Text style={[styles.chipText, !isIll && styles.chipTextSelected]}>No</Text>
            </TouchableOpacity>
          </View>
        </View>

        {/* Notes */}
        <View style={styles.section}>
          <Text style={styles.sectionLabel}>Notes (optional)</Text>
          <TextInput
            style={styles.notesInput}
            placeholder="Any other observations..."
            placeholderTextColor="#94A3B8"
            multiline
            numberOfLines={3}
            value={notes}
            onChangeText={setNotes}
          />
        </View>

        {/* Submit Button */}
        <TouchableOpacity
          style={[styles.submitButton, loading && styles.submitButtonDisabled]}
          onPress={handleSubmit}
          disabled={loading}
        >
          {loading ? (
            <ActivityIndicator color="white" />
          ) : (
            <Text style={styles.submitText}>Save Period</Text>
          )}
        </TouchableOpacity>

      </ScrollView>
    </SafeAreaView>
  );
}